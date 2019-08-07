package me.nicofisi.countingbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableScheduling
class SpringCountingBotApplication

const val MESSAGE_DELETE_DELAY_IN_SECONDS = 10L

fun main(args: Array<String>) {
    runApplication<SpringCountingBotApplication>(*args)
}

@Component
object ApplicationContextGetter : ApplicationContextAware {
    lateinit var context: ApplicationContext
        private set

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }
}