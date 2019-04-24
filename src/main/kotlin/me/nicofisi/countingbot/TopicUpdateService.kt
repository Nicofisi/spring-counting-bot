package me.nicofisi.countingbot

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.collections.HashSet

@Service
class TopicUpdateService(val channelRepository: ChannelRepository) {

    val channelsToUpdate = HashSet<TextChannel>()

    @Scheduled(fixedRate = 10000)
    fun updateChannelTopics() {
        synchronized(channelsToUpdate) {
            channelsToUpdate.forEach { tryUpdateChannelTopic(it) }
            channelsToUpdate.clear()
        }
    }

    fun tryUpdateChannelTopic(channel: TextChannel) {
        if (channel.topic == null) return

        val nextNumber = channelRepository.findByIdOrNull(channel.idLong)?.nextNumber ?: return

        if (!channel.guild.selfMember.hasPermission(channel, Permission.MANAGE_CHANNEL))
            return

        channel.manager.setTopic(
            channel.topic?.replaceFirst(Regex("\\d+"), nextNumber.toString())
        ).queue()
    }
}