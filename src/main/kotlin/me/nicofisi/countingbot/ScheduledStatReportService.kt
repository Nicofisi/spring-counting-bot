package me.nicofisi.countingbot

import me.nicofisi.countingbot.achievements.Achievement
import me.nicofisi.countingbot.achievements.AchievementApplierComponent
import me.nicofisi.countingbot.data.CUnlockedAchievementId
import me.nicofisi.countingbot.data.ChannelRepository
import me.nicofisi.countingbot.data.CountInfoRepository
import me.nicofisi.countingbot.data.UnlockedAchievementRepository
import me.nicofisi.countingbot.util.cardinalString
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.awt.Color
import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder

@Service
class ScheduledStatReportService(
    val channelRepository: ChannelRepository,
    val countInfoRepository: CountInfoRepository,
    val discordController: DiscordController,
    val achievementApplierComponent: AchievementApplierComponent,
    val unlockedAchievementRepository: UnlockedAchievementRepository
) {

    @Scheduled(cron = "0 0 0 * * *")
    fun reportDailyStats() {
        reportStats(StatType.DAY)
    }

    @Scheduled(cron = "1 0 0 * * MON") // 1 second delay
    fun reportWeeklyStats() {
        reportStats(StatType.WEEK)
    }

    fun reportStats(statType: StatType) {
        require(statType in listOf(StatType.DAY, StatType.WEEK)) {
            "Only stats for StatType.DAY and StatType.WEEK can be announced after they end"
        }

        channelRepository.findAll().forEach { countingChannel ->
            val jdaChannel = discordController.jda.getTextChannelById(countingChannel.id) ?: return@forEach

            val yesterday = LocalDate.now().minusDays(1)
            val yesterdayDate = Date.valueOf(yesterday)
            val weekStart = LocalDate.now().minusDays(8) // only for StatType.WEEK
            val weekStartDate = Date.valueOf(weekStart)

            val pageRequest = PageRequest.of(0, 10)

            val top = when (statType) {
                StatType.DAY -> {
                    countInfoRepository.getCountTopByChannelIdAndDate(
                        countingChannel.id,
                        yesterdayDate,
                        pageRequest
                    )
                }
                StatType.WEEK -> {
                    countInfoRepository.getCountTopByChannelIdBetweenDates(
                        countingChannel.id,
                        weekStartDate,
                        yesterdayDate,
                        pageRequest
                    )
                }
                else -> throw IllegalStateException()
            }

            if (top.isEmpty)
                return@forEach

            val embed = EmbedBuilder().apply {
                val yesterdayFormatted = yesterday.format(
                    DateTimeFormatterBuilder()
                        .appendPattern("MMMM ")
                        .appendLiteral(yesterday.dayOfMonth.cardinalString())
                        .appendPattern(", yyyy")
                        .toFormatter()
                )

                setTitle(
                    when (statType) {
                        StatType.DAY -> {
                            "Day reset - summary of $yesterdayFormatted"
                        }
                        StatType.WEEK -> {
                            val weekFormatted = yesterday.format(
                                DateTimeFormatterBuilder()
                                    .appendPattern("w")
                                    .appendLiteral(" of ")
                                    .appendPattern("YYYY")
                                    .toFormatter()
                            )
                            "Week reset - summary of week number $weekFormatted"
                        }
                        else -> throw IllegalStateException()
                    }
                )

                val counts = when (statType) {
                    StatType.DAY -> {
                        countInfoRepository.getCountSumByChannelIdAndDate(
                            countingChannel.id, yesterdayDate
                        )
                    }
                    StatType.WEEK -> {
                        countInfoRepository.getCountSumByChannelIdBetweenDates(
                            countingChannel.id, weekStartDate, yesterdayDate
                        )
                    }
                    else -> throw IllegalStateException()
                }
                val numberAtEnd = countingChannel.nextNumber - 1
                val numberAtStart = numberAtEnd - counts

                appendDescription("Counted $counts times (from $numberAtStart to $numberAtEnd)\n\n")

                val topFieldBuilder = StringBuilder()
                top.withIndex().forEach { (index, topRow) ->
                    val memberMention = jdaChannel.guild.getMemberById(topRow.getUserId())?.asMention
                        ?: "unknown member"

                    topFieldBuilder.append(
                        "**${index + 1}.** $memberMention - ${topRow.getCounts()}\n"
                    )
                }
                addField("Top counters of the ${statType.name.toLowerCase()}", topFieldBuilder.toString(), false)

                setFooter("Page 1 of ${top.totalPages} - new ${statType.name.toLowerCase()} starts now")

                setColor(Color(37, 96, 5))
            }

            jdaChannel.sendMessage(
                MessageBuilder()
                    .setContent(
                        top.take(3)
                            .mapNotNull { jdaChannel.guild.getMemberById(it.getUserId())?.asMention }
                            .joinToString(" ")
                    )
                    .setEmbed(embed.build())
                    .build()
            ).queue()

            if (!top.isEmpty) {
                val topOneRecord = top.first()
                if (!unlockedAchievementRepository.existsById(
                        CUnlockedAchievementId(
                            topOneRecord.getUserId(),
                            countingChannel.id,
                            Achievement.BE_THE_DAILY_BEST_COUNTER.id
                        )
                    )
                ) {
                    val jdaUser = jdaChannel.guild.getMemberById(topOneRecord.getUserId())?.user
                    if (jdaUser != null) {
                        achievementApplierComponent.announceAndSaveAchievement(
                            Achievement.BE_THE_DAILY_BEST_COUNTER,
                            jdaUser,
                            jdaChannel
                        )
                    }
                }
            }

            when (statType) {
                StatType.DAY -> {
                    top.take(3).filterNot {
                        unlockedAchievementRepository.existsById(
                            CUnlockedAchievementId(
                                it.getUserId(),
                                countingChannel.id,
                                Achievement.BE_IN_THE_DAILY_TOP_3.id
                            )
                        )
                    }.forEach top@{
                        achievementApplierComponent.announceAndSaveAchievement(
                            Achievement.BE_IN_THE_DAILY_TOP_3,
                            jdaChannel.guild.getMemberById(it.getUserId())?.user ?: return@top,
                            jdaChannel
                        )
                    }
                }
                StatType.WEEK -> {
                    top.take(5).filterNot {
                        unlockedAchievementRepository.existsById(
                            CUnlockedAchievementId(
                                it.getUserId(),
                                countingChannel.id,
                                Achievement.BE_IN_THE_WEEKLY_TOP_5.id
                            )
                        )
                    }.forEach top@{
                        achievementApplierComponent.announceAndSaveAchievement(
                            Achievement.BE_IN_THE_WEEKLY_TOP_5,
                            jdaChannel.guild.getMemberById(it.getUserId())?.user ?: return@top,
                            jdaChannel
                        )
                    }
                }
                else -> throw IllegalStateException()
            }
        }
    }
}
