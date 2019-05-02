package me.nicofisi.countingbot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class StatType(vararg val aliases: String) {
    DAY("day", "d"),
    WEEK("week", "w"),
    ALL_TIME("all", "alltime", "all-time", "all_time", "forever", "ever", "a")
}

@Component
class StatsComponent(
    private val userRepository: UserRepository,
    private val channelRepository: ChannelRepository,
    private val countInfoRepository: CountInfoRepository,
    private val topicUpdateService: TopicUpdateService,
    private val properties: CountingProperties
) {
    val logger = LoggerFactory.getLogger(javaClass)!!

    /**
     * @return whether the calling method should return now. If false, it should proceed with the logic
     */
    @Transactional
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent): Boolean {
        val message = event.message
        val channel = event.channel
        val guild = event.guild
        val author = message.author
        val member = message.member!!
        val rawContent = message.contentRaw
        val prefix = properties.discordPrefix

        val thisCountingChannel = channelRepository.findByIdOrNull(channel.idLong)

//        if (rawContent.startsWith("<#") && rawContent.slice(20 until (22 + prefix.length)) == "> $prefix") {
        if (rawContent.startsWith("<#") && rawContent.drop(20).take(2) == "> ") {
            val mentionedChannel = rawContent.slice(2 until 20).toLongOrNull()
                ?.let { guild.getTextChannelById(it) }

            fun potentiallyDeleteWithTimedFeedback(feedback: String) {
                if (thisCountingChannel != null) {
                    channel.sendMessage(feedback).queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS) {
                            message.deleteWithFailFeedback()
                        }
                    }
                }
            }

            if (mentionedChannel == null) {
                potentiallyDeleteWithTimedFeedback("Not an ID of a channel in this guild")
                return true
            }

            val mentionedCountingChannel = mentionedChannel.let { channelRepository.findByIdOrNull(it.idLong) }

            if (mentionedCountingChannel == null) {
                potentiallyDeleteWithTimedFeedback("Mentioned channel is not a counting channel")
                return true
            }

            val split = rawContent.split(" ")
            val statTypeInput = split[1].toLowerCase()
            val statType = StatType.values().find { it.aliases.contains(statTypeInput) }
            if (statType == null) {
                potentiallyDeleteWithTimedFeedback("There is no such statistic type")
                return true
            }

            val page = max(1, split.getOrNull(2)?.toIntOrNull() ?: 1) // TODO error feedback?

            val top = when (statType) {
                StatType.ALL_TIME ->
                    countInfoRepository.getAllTimeCountTopByChannelId(
                        mentionedCountingChannel.id,
                        PageRequest.of(page - 1, 15)
                    )
                else -> TODO()
            }

            val embed = EmbedBuilder().apply {
                setTitle(
                    "Top counters of " + when (statType) {
                        StatType.DAY -> "the day"
                        StatType.WEEK -> "the week"
                        StatType.ALL_TIME -> "all time"
                    }
                )
                if (mentionedChannel.id != channel.id)
                    appendDescription("in ${mentionedChannel.asMention}\n\n")

                top.withIndex().forEach { (index, topRow) ->
//                    val memberDisplay = event.guild.getMemberById(topRow.getUserId())?.effectiveName
//                        ?: "*unknown member*"
                    val memberMention = event.guild.getMemberById(topRow.getUserId())?.asMention
                        ?: "unknown member"

                    appendDescription(
                        "**${index + 1}.** $memberMention - ${topRow.getCounts()}\n"
                    )

                    setFooter("Page $page of ${top.totalPages}" + when (statType) {
                        StatType.DAY -> " - day resets in " + TODO()
                        StatType.WEEK -> " - week resets in " + TODO()
                        StatType.ALL_TIME -> ""
                    })

                    setColor(Color(59, 135, 83))
                }
            }


            channel.sendMessage(embed.build()).queue()

            return true
        }

        return false
    }
}
