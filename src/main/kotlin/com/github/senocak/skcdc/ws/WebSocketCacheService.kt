package com.github.senocak.skcdc.ws

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.skcdc.WebsocketIdentifier
import com.github.senocak.skcdc.WsRequestBody
import com.github.senocak.skcdc.logger
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage
import java.io.IOException
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class WebSocketCacheService(
    private val objectMapper: ObjectMapper
) {
    private val log: Logger by logger()
    private val userSessionCache: MutableMap<String, WebsocketIdentifier> = ConcurrentHashMap<String, WebsocketIdentifier>()

    val allWebSocketSession: Map<String, WebsocketIdentifier> get() = userSessionCache

    fun put(data: WebsocketIdentifier) = run { userSessionCache[data.user] = data }
    fun getOrDefault(key: String): WebsocketIdentifier? = userSessionCache.getOrDefault(key = key, defaultValue = null)
    fun deleteSession(key: String) = run {
        val websocketIdentifier: WebsocketIdentifier? = getOrDefault(key = key)
        if (websocketIdentifier?.session == null) {
            log.error("Unable to remove the websocket session; serious error!")
            return@run
        }
        userSessionCache.remove(key = key)
    }
    private fun broadCastMessage(message: String, type: String) {
        val wsRequestBody = WsRequestBody()
            .also {
                it.content = message
                it.date = Instant.now().toEpochMilli()
                it.type = type
            }
        allWebSocketSession.forEach { entry ->
            try {
                entry.value.session!!.sendMessage(TextMessage(objectMapper.writeValueAsString(wsRequestBody)))
            } catch (e: Exception) {
                log.error("Exception while broadcasting: ${e.message}")
            }
        }
    }

    private fun sendMessage(from: String?, to: String, type: String?, payload: String?) {
        val userTo: WebsocketIdentifier? = getOrDefault(key = to)
        if (userTo?.session == null) {
            log.error("User or Session not found in cache for user: $to, returning...")
            return
        }
        val requestBody: WsRequestBody = WsRequestBody()
            .also {
                it.from = from
                it.to = to
                it.date = Instant.now().toEpochMilli()
                it.content = payload
                it.type = type
            }
        try {
            userTo.session!!.sendMessage(TextMessage(objectMapper.writeValueAsString(requestBody)))
        } catch (e: IOException) {
            log.error("Exception while sending message: ${e.message}")
        }
    }

    private fun broadCastAllUserList(to: String): Unit =
        sendMessage(from = null, to = to, type = "online", payload = userSessionCache.keys.joinToString(separator = ","))
}