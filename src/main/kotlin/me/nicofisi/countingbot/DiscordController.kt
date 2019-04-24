package me.nicofisi.countingbot

import net.dv8tion.jda.api.JDABuilder
import org.springframework.stereotype.Controller

@Controller
class DiscordController(countingService: CountingService, properties: CountingProperties) {
    init {
        JDABuilder(properties.discordToken)
            .addEventListeners(countingService)
            .build()
    }
}