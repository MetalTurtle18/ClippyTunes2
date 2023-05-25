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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import java.nio.ByteBuffer

val guildMusicManagers = mutableMapOf<Long, GuildMusicManager>()

class GuildMusicManager(guild: Guild) {
    private val player = audioPlayerManager.createPlayer()
    private val audioManager = guild.audioManager
    private var trackScheduler: TrackScheduler? = null
    private var voiceChannel: VoiceChannel? = null

    private val isStarted: Boolean
        get() = trackScheduler != null
    val queue: List<AudioTrack>
        get() = trackScheduler?.queue ?: emptyList()

    init {
        AudioSourceManagers.registerRemoteSources(audioPlayerManager)
//        AudioSourceManagers.registerLocalSource(audioPlayerManager) TODO: see what this does
        audioManager.sendingHandler = AudioPlayerSendHandler(player)

    }

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

    fun loadItem(identifier: String) {
        if (!isStarted) startup()

        audioPlayerManager.loadItem(identifier, object : AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                trackScheduler?.queue(track)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                playlist.tracks.forEach { trackScheduler?.queue(it) }
            }

            override fun noMatches() {
                TODO("Not yet implemented - no matches")
            }

            override fun loadFailed(exception: FriendlyException) {
                TODO("Not yet implemented - load failed")
            }
        })
    }

    companion object {
        private val audioPlayerManager = DefaultAudioPlayerManager()

        init {
            AudioSourceManagers.registerRemoteSources(audioPlayerManager)
        }
    }
}

class TrackScheduler(private val player: AudioPlayer) : AudioEventListener {

    val queue = mutableListOf<AudioTrack>()

    override fun onEvent(event: AudioEvent?) {
        // TODO("Not yet implemented - on event")
    }

    fun destroy() {
        TODO("I don't know if this will be needed")
    }

    fun queue(track: AudioTrack) {
        queue.add(track)

        if (queue.size == 1) { // If this is the first track, play it
            player.playTrack(track)
        } // TODO else if (nothing is playing b/c the queue has finished)
    }

}

class AudioPlayerSendHandler (private val audioPlayer: AudioPlayer) : AudioSendHandler  {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer {
        return ByteBuffer.wrap(lastFrame?.data)
    }

    override fun isOpus() = true
}