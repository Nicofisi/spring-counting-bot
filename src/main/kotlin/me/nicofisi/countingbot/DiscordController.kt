package me.nicofisi.countingbot

import net.dv8tion.jda.api.JDABuilder
import org.springframework.stereotype.Controller

@Controller
class DiscordController(countingComponent: CountingComponent, properties: CountingProperties) {
    val jda = JDABuilder(properties.discordToken)
        .addEventListeners(countingComponent)
        .build()
}