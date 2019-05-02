package me.nicofisi.countingbot

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface UserRepository : CrudRepository<CUser, Long>

interface ChannelRepository : CrudRepository<CChannel, Long>

interface CountInfoRepository : PagingAndSortingRepository<CCountInfo, CCountInfoId> {
    @Query("SELECT a.id.userId as userId, SUM(a.amount) as counts " +
            "FROM CCountInfo a WHERE a.id.channelId = :channelId GROUP BY a.id.userId ORDER BY SUM(a.amount) DESC")
    fun getAllTimeCountTopByChannelId(channelId: Long, pageable: Pageable): Page<CTopQueryResult>

    fun deleteAllByIdChannelId(channelId: Long): List<CCountInfo>
}