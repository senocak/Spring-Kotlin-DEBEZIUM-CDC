package com.github.senocak.skcdc.ws

import com.github.senocak.skcdc.WebsocketIdentifier
import com.github.senocak.skcdc.logger
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class WebSocketCacheService {
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
}