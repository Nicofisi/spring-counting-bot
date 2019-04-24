package me.nicofisi.countingbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCountingBotApplication

fun main(args: Array<String>) {
    runApplication<SpringCountingBotApplication>(*args)
}
