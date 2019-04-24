package me.nicofisi.countingbot

import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<CUser, Long>

interface ChannelRepository : CrudRepository<CChannel, Long>

interface CountInfoRepository : CrudRepository<CUserCountInfo, CUserCountInfoId> {
    fun deleteAllByChannelId(channelId: Long): List<CUserCountInfo>
}