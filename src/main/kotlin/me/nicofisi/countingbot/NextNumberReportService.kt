package me.nicofisi.countingbot

import me.nicofisi.countingbot.data.ChannelRepository
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.collections.HashSet

@Service
class NextNumberReportService(val channelRepository: ChannelRepository) {
    companion object {
        const val DELAY = 10000
    }

    private val channelsToUpdate = HashSet<TextChannel>()
    private val channelsToLastUpdated = HashMap<TextChannel, Long>()

    @Scheduled(fixedRate = 10000)
    fun updateChannelTopics() {
        synchronized(channelsToUpdate) {
            synchronized(channelsToLastUpdated) {
                channelsToUpdate
                    .filter {
                        channelsToLastUpdated[it]?.plus(DELAY) ?: 0 < System.currentTimeMillis()
                    }
                    .forEach { tryUpdateChannelTopic(it) }

                channelsToUpdate.clear()

                val iter = channelsToLastUpdated.iterator()
                while (iter.hasNext()) {
                    val (_, lastUpdated) = iter.next()
                    if (lastUpdated < DELAY)
                        iter.remove()
                }
            }
        }
    }

    fun scheduleForUpdate(jdaChannel: TextChannel) {
        if (channelsToLastUpdated[jdaChannel]?.plus(DELAY) ?: 0 < System.currentTimeMillis()) {
            channelsToLastUpdated += (jdaChannel to System.currentTimeMillis())
            tryUpdateChannelTopic(jdaChannel)
        } else {
            channelsToUpdate.add(jdaChannel)
        }
    }

    fun tryUpdateChannelTopic(jdaChannel: TextChannel) {
        if (jdaChannel.topic == null) return

        val nextNumber = channelRepository.findByIdOrNull(jdaChannel.idLong)?.nextNumber ?: return

        if (!jdaChannel.guild.selfMember.hasPermission(jdaChannel, Permission.MANAGE_CHANNEL))
            return

        jdaChannel.manager.setTopic(
            jdaChannel.topic?.replaceFirst(Regex("\\d+"), nextNumber.toString())
        ).queue()
    }
}