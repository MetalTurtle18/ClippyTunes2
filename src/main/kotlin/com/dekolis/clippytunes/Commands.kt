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

import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import kotlin.time.Duration

typealias CommandContext = suspend CoroutineEventListener.(GenericCommandInteractionEvent) -> Unit
typealias CommandCheckContext = suspend CoroutineEventListener.(GenericCommandInteractionEvent) -> Boolean

val commands = listOf(
    CTCommand(
        name = "join",
        description = "Have the bot join the voice channel you are currently in",
        checks = listOf(
            CTCommand.Check.memberInVoiceChannel,
        ),
    ) {
        val guild = it.guild
        val channel = it.member!!.voiceState!!.channel!! as VoiceChannel
        val musicManager = guildMusicManagers[guild!!.idLong]!!
        musicManager.joinVoiceChannel(channel)
        it.reply_("Joined ${channel.asMention}").queue()
    },
    CTCommand(
        name = "queue",
        description = "Add a song to the queue",
        checks = listOf(
            CTCommand.Check.memberInVoiceChannel,
        ),
        options = listOf(
            CTCommand.Option(
                name = "song",
                description = "The song to add to the queue",
                type = OptionType.STRING,
                required = true,
                simpleAutocomplete = listOf("option1", "option2"),
            )
        )
    ) {

        it.reply_(it.getOption("song")!!.asString).queue()
    }
)

class CTCommand(
    val name: String,
    val description: String,
    val timeout: Duration? = null,
    val checks: List<Check> = emptyList(),
    val options: List<Option> = emptyList(),
    val slashCommandData: SlashCommandData.() -> Unit = {},
    val commandHandler: CommandContext
) {
    class Check(val errorMessage: String, val check: CommandCheckContext) {
        companion object {
            val memberInVoiceChannel = Check("oopsie poopsie" /*TODO*/) { event: GenericCommandInteractionEvent ->
                event.member?.voiceState?.inAudioChannel() ?: false
            }
        }
    }

    class Option(
        val name: String,
        val description: String,
        val type: OptionType = OptionType.STRING,
        val required: Boolean = false,
        private val simpleAutocomplete: List<String> = emptyList(),
        private val customAutocomplete: suspend CoroutineEventListener.(CommandAutoCompleteInteractionEvent) -> Unit = {}
    ) {
        val autocomplete: suspend CoroutineEventListener.(CommandAutoCompleteInteractionEvent) -> Unit
            get() =
                if (simpleAutocomplete.isEmpty()) {
                    customAutocomplete
                } else {
                    { event ->
                        event.replyChoiceStrings(simpleAutocomplete).queue()
                    }
                }

        val doAutoComplete: Boolean
            get() = simpleAutocomplete.isNotEmpty() || customAutocomplete != {}
    }
}