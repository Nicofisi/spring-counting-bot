package me.nicofisi.countingbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SpringCountingBotApplication

const val MESSAGE_DELETE_DELAY_IN_SECONDS = 10L

fun main(args: Array<String>) {
    runApplication<SpringCountingBotApplication>(*args)
}
