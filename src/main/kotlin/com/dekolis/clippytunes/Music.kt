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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

val guildMusicManagers = mutableMapOf<Long, GuildMusicManager>()

class GuildMusicManager(guild: Guild) {
    private val player = audioPlayerManager.createPlayer()
    private val audioManager = guild.audioManager
    private var trackScheduler: TrackScheduler? = null
    private var voiceChannel: VoiceChannel? = null

    fun startup() {
        trackScheduler = TrackScheduler(player)
        player.addListener(trackScheduler)
    }

    fun destroy() {
        trackScheduler?.destroy()
        player.removeListener(trackScheduler)
        trackScheduler = null
    }

    fun joinVoiceChannel(channel: VoiceChannel) {
        voiceChannel = channel
        audioManager.openAudioConnection(voiceChannel)
    }

    companion object {
        private val audioPlayerManager = DefaultAudioPlayerManager()

        init {
            AudioSourceManagers.registerRemoteSources(audioPlayerManager)
        }
    }
}

class TrackScheduler(private val player: AudioPlayer) : AudioEventListener {
    override fun onEvent(event: AudioEvent?) {
        TODO("Not yet implemented")
    }

    fun destroy() {
        TODO("I don't know if this will be needed")
    }

}