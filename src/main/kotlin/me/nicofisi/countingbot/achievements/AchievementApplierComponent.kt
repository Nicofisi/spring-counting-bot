package me.nicofisi.countingbot.achievements

import me.nicofisi.countingbot.data.CUnlockedAchievement
import me.nicofisi.countingbot.data.CUnlockedAchievementId
import me.nicofisi.countingbot.data.UnlockedAchievementRepository
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import org.springframework.stereotype.Component
import java.awt.Color
import java.sql.Timestamp
import java.time.LocalDateTime

@Component
class AchievementApplierComponent(private val unlockedAchievementRepository: UnlockedAchievementRepository) {
    fun announceAndSaveAchievement(
        achievement: Achievement,
        jdaUser: User,
        jdaChannel: TextChannel,
        unlockedAmount: Int? = null
    ) {
        val unlocked = unlockedAmount ?: {
            unlockedAchievementRepository
                .findAllByIdUserIdAndIdChannelId(jdaUser.idLong, jdaChannel.idLong).size + 1
        }()

        jdaChannel.sendMessage(
            EmbedBuilder()
                .setAuthor(jdaUser.asTag, null, jdaUser.effectiveAvatarUrl)
                .setTitle("Achievement unlocked")
                .setDescription(achievement.description)
                .setFooter(
                    "Unlocked $unlocked of ${Achievement.values().size} achievements"
                )
                .setColor(Color(156, 160, 44))
                .build()
        ).queue()

        unlockedAchievementRepository.save(
            CUnlockedAchievement(
                CUnlockedAchievementId(
                    jdaUser.idLong,
                    jdaChannel.idLong,
                    achievement.id
                ),
                Timestamp.valueOf(LocalDateTime.now())
            )
        )
    }
}
