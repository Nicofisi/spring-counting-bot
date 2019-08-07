package me.nicofisi.countingbot.data

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.sql.Date

interface UserRepository : CrudRepository<CUser, Long>

interface ChannelRepository : CrudRepository<CChannel, Long>

interface CountInfoRepository : PagingAndSortingRepository<CCountInfo, CCountInfoId> {
    @Query("SELECT a.id.userId AS userId, SUM(a.amount) AS counts FROM CCountInfo a " +
            "WHERE a.id.channelId = :channelId GROUP BY a.id.userId ORDER BY SUM(a.amount) DESC")
    fun getAllTimeCountTopByChannelId(channelId: Long, pageable: Pageable): Page<CTopQueryResult>

    @Query("SELECT a.id.userId AS userId, SUM(a.amount) AS counts FROM CCountInfo a " +
            "WHERE a.id.channelId = :channelId AND a.id.date = :date GROUP BY a.id.userId ORDER BY SUM(a.amount) DESC")
    fun getCountTopByChannelIdAndDate(channelId: Long, date: Date, pageable: Pageable): Page<CTopQueryResult>

    @Query("SELECT a.id.userId AS userId, SUM(a.amount) AS counts FROM CCountInfo a " +
            "WHERE a.id.channelId = :channelId AND a.id.date BETWEEN :startDate AND :endDate GROUP BY a.id.userId ORDER BY SUM(a.amount) DESC")
    fun getCountTopByChannelIdBetweenDates(channelId: Long, startDate: Date, endDate: Date, pageable: Pageable): Page<CTopQueryResult>

    @Query("SELECT SUM(a.amount) FROM CCountInfo a WHERE a.id.channelId = :channelId AND a.id.date = :date")
    fun getCountSumByChannelIdAndDate(channelId: Long, date: Date): Int

    @Query("SELECT SUM(a.amount) FROM CCountInfo a WHERE a.id.channelId = :channelId AND a.id.userId = :userId")
    fun getCountSumByChannelIdAndUserId(channelId: Long, userId: Long): Int

    fun getAllByIdChannelIdAndIdUserId(channelId: Long, userId: Long): List<CCountInfo>

    fun deleteAllByIdChannelId(channelId: Long)
}

interface UnlockedAchievementRepository : PagingAndSortingRepository<CUnlockedAchievement, CUnlockedAchievementId> {
    fun findAllByIdUserIdAndIdChannelId(userId: Long, channelId: Long): List<CUnlockedAchievement>

    fun deleteAllByIdChannelId(channelId: Long)
}