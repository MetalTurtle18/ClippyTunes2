package com.dekolis.clippytunes

import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.entities.Activity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ClippyTunes {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

fun main(args: Array<String>) {
    val jda = light(
        token = config.discord.botToken
    ) {
        setActivity(
            Activity.of(
                Activity.ActivityType.valueOf(config.discord.status.activity),
                config.discord.status.description
            )
        )
    }.awaitReady()
    ClippyTunes.logger.info("Bot has started!")
}