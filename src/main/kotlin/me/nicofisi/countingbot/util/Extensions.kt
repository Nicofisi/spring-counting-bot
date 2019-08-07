package me.nicofisi.countingbot.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message
import java.time.Duration

fun Message.deleteWithFailFeedback(delay: Duration? = null) {
    require(channelType == ChannelType.TEXT)

    GlobalScope.launch {
        if (delay != null)
            delay(delay.toMillis())

        if (guild.selfMember.hasPermission(textChannel, Permission.MESSAGE_MANAGE)) {
            delete().queue()
        } else {
            channel.sendMessage("Error: I lack message delete permissions").queue()
        }
    }
}

fun Int.cardinalString(): String {
    require(this >= 0)
    return toString() + when (this % 100) {
        11, 12, 13 -> "th"
        else -> arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")[this % 10]
    }
}