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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

val config: Config by lazy {
    Json.decodeFromString(File("config.json").readText())
}

@Serializable
data class Config(
    val discord: Discord,
    @SerialName("youtube") val youTube: YouTube,
    val spotify: Spotify
)

@Serializable
data class Discord(
    @SerialName("bot-token") val botToken: String,
    val status: Status,
    val guilds: List<Long>
) {
    init {
        require(guilds.isNotEmpty())
    }
}

@Serializable
data class Status(
    val activity: String,
    val description: String
) {
    init {
        require(activity in listOf("PLAYING", "WATCHING", "LISTENING", "STREAMING", "COMPETING"))
    }
}

@Serializable
data class YouTube(@SerialName("api-key") val apiKey: String)

@Serializable
data class Spotify(@SerialName("api-key") val apiKey: String)