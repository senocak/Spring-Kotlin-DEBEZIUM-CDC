package com.github.senocak.skcdc.ws

import com.github.senocak.skcdc.WebsocketIdentifier
import com.github.senocak.skcdc.logger
import com.github.senocak.skcdc.split
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.PongMessage
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.AbstractWebSocketHandler

@Component
class WebsocketChannelHandler(
    private val webSocketCacheService: WebSocketCacheService,
): AbstractWebSocketHandler() {
    private val log: Logger by logger()
    private val lock = ReentrantLock(true)

    override fun afterConnectionEstablished(session: WebSocketSession): Unit =
        lock.withLock {
            runCatching {
                log.info("Websocket connection established. Path: ${session.uri!!.path}")
                if (session.uri == null)
                    log.error("Unable to retrieve the websocket session; serious error!").also { return }
                val email: String = getEmail(query = session.uri!!.query)
                WebsocketIdentifier(user = email, session = session)
                    .also { log.info("Websocket session established: $it") }
                    .run { webSocketCacheService.put(data = this) }
            }.onFailure {
                log.error("A serious error has occurred with websocket post-connection handling. Ex: ${it.message}")
            }
        }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus): Unit =
        lock.withLock {
            log.info("Websocket connection closed. Path: ${session.uri!!.path}")
            runCatching {
                if (session.uri == null)
                    log.error("Unable to retrieve the websocket session; serious error!").also { return }
                val email: String = getEmail(query = session.uri!!.query)
                webSocketCacheService.deleteSession(key = email)
                    .also { log.info("Websocket for $email has been closed") }
            }.onFailure {
                log.error("Error occurred while closing websocket channel:${it.message}")
            }
        }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        when (message) {
            is PingMessage -> log.info("PingMessage: $message")
            is PongMessage -> log.info("PongMessage: $message")
            is BinaryMessage -> log.info("BinaryMessage: $message")
            is TextMessage -> log.info("TextMessage: ${message.payload}")
            else -> session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Not supported"))
                .also { log.error("Not supported. ${message.javaClass}") }
        }
    }

    private fun getEmail(query: String): String  {
        val queryParams: Map<String, String> = query.getQueryParams() ?: throw Exception("QueryParams can not be empty")
        return queryParams["email"] ?: throw Exception("Email can not be empty")
    }

    private fun String.getQueryParams(): Map<String, String>? {
        val queryParams: MutableMap<String, String> = LinkedHashMap()
        return when {
            this.isEmpty() -> null
            else -> {
                val split: Array<String>? = this.split(delimiter = "&")
                if (!split.isNullOrEmpty())
                    for (param: String in split) {
                        val paramArray: Array<String>? = param.split(delimiter = "=")
                        queryParams[paramArray!![0]] = paramArray[1]
                    } else {
                    val paramArray: Array<String>? = this.split(delimiter = "=")
                    queryParams[paramArray!![0]] = paramArray[1]
                }
                queryParams
            }
        }
    }
}