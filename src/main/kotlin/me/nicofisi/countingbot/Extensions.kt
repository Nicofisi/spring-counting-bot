package me.nicofisi.countingbot

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Message

fun Message.deleteWithFailFeedback() {
    require(channelType == ChannelType.TEXT)

    if (guild.selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
        delete().queue()
    } else {
        channel.sendMessage("Error: I lack message delete permissions").queue()
    }
}