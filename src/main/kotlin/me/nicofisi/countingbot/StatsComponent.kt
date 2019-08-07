package me.nicofisi.countingbot

import me.nicofisi.countingbot.data.ChannelRepository
import me.nicofisi.countingbot.data.CountInfoRepository
import me.nicofisi.countingbot.data.UserRepository
import me.nicofisi.countingbot.util.deleteWithFailFeedback
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
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
    private val nextNumberReportService: NextNumberReportService,
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
            val statType = StatType.values().find { statTypeInput in it.aliases }
            if (statType == null) {
                potentiallyDeleteWithTimedFeedback("There is no such statistic type")
                return true
            }

            val page = max(1, split.getOrNull(2)?.toIntOrNull() ?: 1) // TODO error feedback?
            val pageRequest = PageRequest.of(page - 1, 10)

            val (top, endsOnMidnightAt) = when (statType) {
                StatType.DAY -> {
                    Pair(
                        countInfoRepository.getCountTopByChannelIdAndDate(
                            mentionedCountingChannel.id,
                            Date.valueOf(LocalDate.now()),
                            pageRequest
                        ),
                        LocalDate.now().plusDays(1)
                    )
                }
                StatType.WEEK -> {
                    val startDate = LocalDate.now().with(ChronoField.DAY_OF_WEEK, 1)
                    val endDate = LocalDate.now().with(ChronoField.DAY_OF_WEEK, 7)

                    Pair(
                        countInfoRepository.getCountTopByChannelIdBetweenDates(
                            mentionedCountingChannel.id,
                            Date.valueOf(startDate),
                            Date.valueOf(endDate),
                            pageRequest
                        ),
                        endDate.plusDays(1)
                    )
                }
                StatType.ALL_TIME ->
                    Pair(
                        countInfoRepository.getAllTimeCountTopByChannelId(
                            mentionedCountingChannel.id,
                            pageRequest
                        ),
                        null
                    )
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
                    val memberMention = event.guild.getMemberById(topRow.getUserId())?.asMention
                        ?: "unknown member"

                    appendDescription(
                        "**${index + 1}.** $memberMention - ${topRow.getCounts()}\n"
                    )
                }


                val resetsIn = endsOnMidnightAt?.let {
                    val now = LocalDateTime.now()
                    val later = it.atTime(0, 0)

                    val days = ChronoUnit.DAYS.between(now, later)
                    val hours = ChronoUnit.HOURS.between(now, later) - days * 24
                    val minutes = ChronoUnit.MINUTES.between(now, later) - days * 24 - hours * 60
                    val seconds = ChronoUnit.SECONDS.between(now, later) - days * 24 - hours * 60 - minutes * 60

                    var str = ""

                    var addedUnits = 0

                    for ((index, value) in listOf(days, hours, minutes, seconds).withIndex()) {
                        if (value != 0L) {
                            val unit = when (index) {
                                0 -> "day"
                                1 -> "hour"
                                2 -> "minute"
                                else -> "second"
                            }
                            if (addedUnits == 1)
                                str += " and "
                            str += "$value ${if (value == 1L) unit else unit + "s"}"
                            addedUnits++
                        }

                        if (addedUnits == 2) break
                    }

                    str
                }

                setFooter(
                    "Page $page of ${top.totalPages}" + when (statType) {
                        StatType.DAY -> " - day resets in $resetsIn"
                        StatType.WEEK -> " - week resets in $resetsIn"
                        StatType.ALL_TIME -> ""
                    }
                )

                setColor(Color(59, 135, 83))
            }

            channel.sendMessage(embed.build()).queue()

            return true
        }

        return false
    }
}
