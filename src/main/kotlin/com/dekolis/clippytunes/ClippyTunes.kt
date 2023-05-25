/*
 * ClippyTunes is a Discord music bot written in Kotlin
 * Copyright (C) 2023 MetalTurtle18
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dekolis.clippytunes

import dev.minn.jda.ktx.events.CoroutineEventManager
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.events.onCommandAutocomplete
import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.jdabuilder.cache
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
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
    // All this coroutine stuff is from https://github.com/MinnDevelopment/strumbot/blob/master/src/main/kotlin/strumbot/main.kt
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

    // Listener for when the bot joins a guild or when guilds are initialized on startup
    manager.listener<GenericGuildEvent> { event ->
        if (event !is GuildReadyEvent && event !is GuildJoinEvent) return@listener

        val guild = event.guild
        logger.info("Guild ready for startup: ${guild.name}. Checking to see if it is in the config...")

        if (guild.idLong !in config.discord.guilds) {
            logger.info("Guild ${guild.name} is not in the config. Skipping.")
            return@listener
        }
        logger.info("Guild ${guild.name} is in the config. Updating commands...")

        // Update commands for this guild
        guild.updateCommands {
            commands.forEach {
                slash(it.name, it.description) {
                    it.options.forEach { option ->
                        addOption(option.type, option.name, option.description, option.required, option.doAutoComplete)
                    }
                    it.slashCommandData(this)
                }
                logger.info("Registered command ${it.name} to guild ${guild.name}")
            }
        }.queue()
        logger.info("Commands updated for guild ${guild.name}")

        // Initialize the guild for music
        logger.info("Initializing music for guild ${guild.name}...")
        guildMusicManagers += guild.idLong to GuildMusicManager(guild)
        logger.info("Initialized music for guild ${guild.name}")
    }

    logger.info("Initializing JDA")
    val jda = light(
        token = config.discord.botToken,
        enableCoroutines = false,
        intents = listOf(
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.GUILD_MEMBERS
        )
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
        setMemberCachePolicy(MemberCachePolicy.VOICE)
        cache += CacheFlag.VOICE_STATE
    }

    jda.awaitReady()
    logger.info("Initialized JDA")

    logger.info("Registering command listeners")
    commands.forEach {
        it.options.forEach { option ->
            jda.onCommandAutocomplete(
                name = it.name,
                option = option.name,
                consumer = option.autocomplete
            )
        }

        jda.onCommand(it.name, it.timeout) { event ->
            logger.info("Received command ${it.name} from ${event.user.name}#${event.user.discriminator} in ${event.guild?.name ?: "DMs"}")
            for (check in it.checks) {
                if (!check.check(this, event)) {
                    event.reply_(check.errorMessage).queue()
                    logger.info("Command ${it.name} failed checks: ${check.errorMessage}")
                    return@onCommand
                }
            }

            // If it passed all the checks
            logger.info("Command ${it.name} passed checks. Executing...")
            it.commandHandler(this, event)
        }
    }
    logger.info("Registered command listeners")

    manager.listener<ShutdownEvent> {
        supervisor.cancel()
    }

    // https://github.com/MinnDevelopment/strumbot/blob/master/src/main/kotlin/strumbot/main.kt
    supervisor.invokeOnCompletion {
        if (it != null && it !is CancellationException) {
            logger.error("Supervisor failed with unexpected error", it)
        } else {
            logger.info("Shutting down")
        }
        jda.shutdown()
    }
}