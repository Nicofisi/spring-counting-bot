package me.nicofisi.countingbot.data

import java.io.Serializable
import java.sql.Date
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "users")
data class CUser(
    @Id val id: Long,
    var cachedName: String
)

@Entity
@Table(name = "channels")
data class CChannel(
    @Id val id: Long,
    var lastUserId: Long?,
    var cachedName: String,
    var cachedGuildName: String,
    var nextNumber: Int = 1
)

@Entity
@Table(name = "count_info")
data class CCountInfo(
    @EmbeddedId val id: CCountInfoId,
    var amount: Int
)

@Embeddable
data class CCountInfoId(
    val userId: Long,
    val channelId: Long,
    @Column(name = "at_date") val date: Date
) : Serializable

@Entity
@Table(name = "unlocked_achievements")
data class CUnlockedAchievement(
    @EmbeddedId val id: CUnlockedAchievementId,
    val unlockedAt: Timestamp
)

@Embeddable
data class CUnlockedAchievementId(
    val userId: Long,
    val channelId: Long,
    val achievementId: Int
) : Serializable

interface CTopQueryResult {
    fun getUserId(): Long
    fun getCounts(): Int
}
