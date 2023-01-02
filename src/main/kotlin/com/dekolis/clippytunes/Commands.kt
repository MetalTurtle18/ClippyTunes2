package com.dekolis.clippytunes

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import kotlin.time.Duration

val commands = listOf(
    CTCommand("join", "Have the bot join the voice channel you are currently in") {
        it.reply_("asdf").queue()
    }
)

class CTCommand(
    val name: String,
    val description: String,
    val timeout: Duration? = null,
    val data: SlashCommandData.() -> Unit = {},
    val handler: suspend CoroutineEventListener.(GenericCommandInteractionEvent) -> Unit
)