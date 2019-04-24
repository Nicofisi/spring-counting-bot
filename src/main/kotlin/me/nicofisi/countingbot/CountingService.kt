package me.nicofisi.countingbot

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

@Service
class CountingService(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val countInfoRepository: CountInfoRepository,
    private val topicUpdateService: TopicUpdateService,
    private val properties: CountingProperties
) : ListenerAdapter() {
    val logger = Logger.getLogger(javaClass.canonicalName)!!

    override fun onReady(event: ReadyEvent) {
        logger.info("Listening for Discord events")
    }

    @Transactional
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val message = event.message
        val channel = event.channel
        val guild = event.guild
        val author = message.author
        val member = message.member!!
        val rawContent = message.contentRaw
        val prefix = properties.discordPrefix

        if (author == event.jda.selfUser) return

        val countingChannel = channelRepository.findByIdOrNull(channel.idLong)

        fun requireOpWithFeedback(): Boolean {
            return if (properties.discordAdminIds.contains(author.idLong) ||
                member.hasPermission(Permission.MANAGE_SERVER)
            ) {
                true
            } else {
                channel.sendMessage("${author.asMention}, you need the Manage Server permission to use this command")
                    .queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)

                        if (countingChannel == null)
                            message.delete().queueAfter(10, TimeUnit.SECONDS)
                        else
                            message.delete().queue()
                    }
                false
            }
        }

        when (rawContent) {
            "${prefix}check" -> {
                if (!requireOpWithFeedback()) return

                channel.sendMessage(
                    "Is this channel a counting channel: ${countingChannel != null}"
                ).queue()
                return
            }
            "${prefix}add" -> {
                if (!requireOpWithFeedback()) return

                channelRepository.save(CChannel(channel.idLong, null, channel.name, guild.name))
                channel.sendMessage("Done").queue()
                return
            }
            "${prefix}delete-all-counting-stats-for-this-channel-no-undo-seriously-this-command-is-not-a-joke" -> {
                if (!requireOpWithFeedback()) return

                countingChannel?.run {
                    channelRepository.delete(this)
                    countInfoRepository.deleteAllByChannelId(id)
                }
                channel.sendMessage("Done").queue()
                return
            }
        }

        // if this channel is registered as a counting channel
        if (countingChannel != null) {
            val expectedNumber = countingChannel.nextNumber

            if (author.isBot
                || countingChannel.lastUserId == author.idLong
                || !CountLogic.messageMatchesNumber(rawContent, expectedNumber)
            ) {
                message.deleteWithFailFeedback()
                return
            }

            countingChannel.run {
                lastUserId = author.idLong
                nextNumber++
                cachedName = channel.name
                cachedGuildName = guild.name
            }

            channelRepository.save(countingChannel)

            val countingUser = userRepository.findByIdOrNull(author.idLong) ?: CUser(author.idLong, "")
            countingUser.cachedName = author.name
            userRepository.save(countingUser)

            val countInfoId = CUserCountInfoId(author.idLong, channel.idLong, Date(System.currentTimeMillis()))
            val countInfo = countInfoRepository.findByIdOrNull(countInfoId) ?: CUserCountInfo(countInfoId, 0)
            countInfo.amount++
            countInfoRepository.save(countInfo)

            topicUpdateService.channelsToUpdate.add(channel)
        }
    }

    override fun onTextChannelUpdateTopic(event: TextChannelUpdateTopicEvent) {
        val old = event.oldTopic
        val new = event.newTopic ?: return

        if (old != null && new.length == old.length) { // there is a wasted run for 9 -> 10, 99 -> 100 etc.
            new.forEach outer@{ c1 ->
                old.forEach { c2 ->
                    if (c1 != c2 && !c1.isDigit() && !c2.isDigit()) return@outer
                }
            }
            return // only digits changed, aka probably it's us who changed the topic
        }

        topicUpdateService.tryUpdateChannelTopic(event.channel)
    }
}