package com.dekolis.clippytunes

import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.jdabuilder.light
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Integer.max
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.minutes

val logger: Logger = LoggerFactory.getLogger("ClippyTunes")

fun getThreadCount(): Int = max(2, ForkJoinPool.getCommonPoolParallelism())

private val pool = Executors.newScheduledThreadPool(getThreadCount()) {
    thread(start = false, name = "Worker-Thread", isDaemon = true, block = it::run)
}

fun main(args: Array<String>) {
    // All this coroutine shit is from https://github.com/MinnDevelopment/strumbot/blob/master/src/main/kotlin/strumbot/main.kt
    // Use the global thread pool for coroutine dispatches
    val dispatcher = pool.asCoroutineDispatcher()
    // Using a SupervisorJob allows coroutines to fail without cancelling all other jobs
    val supervisor = SupervisorJob()
    // Implement a logging exception handler for uncaught throws in launched jobs
    val handler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException)
            logger.error("Uncaught exception in coroutine", throwable)
        if (throwable is Error) {
            supervisor.cancel()
            throw throwable
        }
    }

    // Create our coroutine scope
    val context = dispatcher + supervisor + handler
    val scope = CoroutineScope(context)

    // Create a coroutine manager with this scope and a default event timeout of 1 minute
    val manager = CoroutineEventManager(scope, 1.minutes)

    manager.initCommands()

    manager.listener<ShutdownEvent> {
        supervisor.cancel()
    }

    logger.info("Initializing JDA")
    val jda = light(
        token = config.discord.botToken,
        enableCoroutines = false
    ) {
        setEventManager(manager)
        setCallbackPool(pool)
        setGatewayPool(pool)
        setRateLimitPool(pool)
        setActivity(
            Activity.of(
                Activity.ActivityType.valueOf(config.discord.status.activity),
                config.discord.status.description
            )
        )
    }

    jda.awaitReady()
    logger.info("Initialized JDA")

    logger.info("Registering command listeners")
    commands.forEach {
        jda.onCommand(it.name, it.timeout, it.handler)
    }
    logger.info("Registered command listeners")

    // More stuff from that other bot; I don't know what it's doing
    supervisor.invokeOnCompletion {
        if (it != null && it !is CancellationException) {
            logger.error("Supervisor failed with unexpected error", it)
        } else {
            logger.info("Shutting down")
        }
        jda.shutdown()
    }

    System.gc() // I don't know why this is here, but it is funny and the other person did it, so I'm leaving it
}

private fun CoroutineEventManager.initCommands() = listener<GenericGuildEvent> { event ->
    if (event !is GuildReadyEvent && event !is GuildJoinEvent) return@listener

    val guild = event.guild
    logger.info("Guild ready for startup: ${guild.name}. Checking to see if it is in the config...")

    if (guild.idLong !in config.discord.guilds) {
        logger.info("Guild ${guild.name} is not in the config. Skipping...")
        return@listener
    }
    logger.info("Guild is in the config. Updating commands...")

    guild.updateCommands {
        commands.forEach {
            slash(it.name, it.description, it.data) // Register the command for the guild
        }
    }.queue()
}