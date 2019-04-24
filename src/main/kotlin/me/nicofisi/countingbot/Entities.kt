package me.nicofisi.countingbot

import java.io.Serializable
import java.sql.Date
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
data class CUserCountInfo(
    @EmbeddedId val id: CUserCountInfoId,
    var amount: Int
)

@Embeddable
data class CUserCountInfoId(
    val userId: Long,
    val channelId: Long,
    @Column(name = "at_date") val date: Date
) : Serializable