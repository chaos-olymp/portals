package de.chaosolymp.portals.bungee

import com.google.gson.Gson
import com.google.gson.JsonObject
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.awt.Color
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit

object DebugMessenger {
    internal val targetPlayers: MutableSet<ProxiedPlayer> = mutableSetOf()
    private val httpClient = HttpClient.newHttpClient()
    private val gson = Gson()

    fun exception(label: String, exception: Exception) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        exception.printStackTrace(printWriter)

        val postRequest = createHasteRequest(stringWriter.toString())
        val httpFuture = httpClient.sendAsync(postRequest, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))

        var url: String? = null

        try {
            val response = httpFuture.get(5, TimeUnit.SECONDS)
            val responseBody = response.body()
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val key = jsonObject["key"].asString
            url = "https://www.toptal.com/developers/hastebin/$key"
        } catch (ex: Exception) {
            // Do nothing / url is null
        }

        val componentBuilder = ComponentBuilder("[PORTALS - DEBUG] ")
            .color(ChatColor.of(Color(155, 89, 182)))
            .append("[CRIT] ")
            .color(ChatColor.DARK_RED)
            .append("$label: ")
            .color(ChatColor.RED)
            .bold(true)
            .append("Exception of type ${exception.javaClass.name} - ${exception.message}")
            .color(ChatColor.RED)
            .bold(false)

        if(url != null) {
            componentBuilder.append(" - ")
                .color(ChatColor.RED)
            componentBuilder.append("Click here to view stack trace")
                .event(ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .color(ChatColor.RED).underlined(true)
        }

        val components = componentBuilder.create()

        sendToAllPlayers(*components)
    }

    private fun createHasteRequest(content: String) = HttpRequest.newBuilder(URI.create("https://www.toptal.com/developers/hastebin/documents"))
        .POST(HttpRequest.BodyPublishers.ofString(content))
        .build()

    fun critical(label: String, text: String) {
        val components = ComponentBuilder("[PORTALS - DEBUG] ")
            .color(ChatColor.of(Color(155, 89, 182)))
            .append("[CRIT] ")
            .color(ChatColor.DARK_RED)
            .append("$label: ")
            .color(ChatColor.RED)
            .bold(true)
            .append(text)
            .color(ChatColor.RED)
            .bold(false)
            .create()

        sendToAllPlayers(*components)
    }

    fun warning(label: String, text: String) {
        val components = ComponentBuilder("[PORTALS - DEBUG] ")
            .color(ChatColor.of(Color(155, 89, 182)))
            .append("[WARN] ")
            .color(ChatColor.GOLD)
            .append("$label: ")
            .color(ChatColor.GOLD)
            .bold(true)
            .append(text)
            .color(ChatColor.GOLD)
            .bold(false)
            .create()

        sendToAllPlayers(*components)
    }

    fun info(label: String, text: String) {
        val components = ComponentBuilder("[PORTALS - DEBUG] ")
            .color(ChatColor.of(Color(155, 89, 182)))
            .append("[INFO] ")
            .color(ChatColor.BLUE)
            .append("$label: ")
            .color(ChatColor.BLUE)
            .bold(true)
            .append(text)
            .color(ChatColor.BLUE)
            .bold(false)
            .create()

        sendToAllPlayers(*components)
    }

    fun verbose(label: String, text: String) {
        val components = ComponentBuilder("[PORTALS - DEBUG] ")
            .color(ChatColor.of(Color(155, 89, 182)))
            .append("[VERB] ")
            .color(ChatColor.GRAY)
            .append("$label: ")
            .color(ChatColor.GRAY)
            .bold(true)
            .append(text)
            .color(ChatColor.GRAY)
            .bold(false)
            .create()

        sendToAllPlayers(*components)
    }

    private fun sendToAllPlayers(vararg components: BaseComponent) {
        targetPlayers.forEach { player ->
            // Send as ChatMessageType.SYSTEM (message is also present if the player disabled the Chat in the client settings)
            player.sendMessage(ChatMessageType.SYSTEM, *components)
        }
    }
}