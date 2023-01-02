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
)

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