package me.nicofisi.countingbot

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("counting")
class CountingProperties {
    lateinit var discordToken: String

    lateinit var discordPrefix: String

    lateinit var discordAdminIds: List<Long>
}