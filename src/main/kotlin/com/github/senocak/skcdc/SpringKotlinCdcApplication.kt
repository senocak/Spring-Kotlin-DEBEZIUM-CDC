package com.github.senocak.skcdc

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.senocak.skcdc.ws.WebSocketCacheService
import java.util.Optional
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.util.StringUtils
import org.springframework.web.socket.TextMessage

@SpringBootApplication
@EnableScheduling
class SpringKotlinCdcApplication(
    private val userRepository: UserRepository,
    private val webSocketCacheService: WebSocketCacheService,
    private val objectMapper: ObjectMapper
){
    private val log: Logger by logger()

    @Scheduled(cron = "0/10 * * ? * *")
    fun updateInterval() {
        val findById: Optional<User> = userRepository.findById(UUID.fromString("2cb9374e-4e52-4142-a1af-16144ef4a27d"))
        if (findById.isEmpty) {
            log.error("User not found in database")
            return
        }
        val user: User = findById.get()
        val rnds: Int = (100_000..10_000_000).random()

        user.name = "Lorem Ipsum $rnds"
        user.email = "lorem$rnds@ipsum.com"
        userRepository.save(user)
    }

    @KafkaListener(topics = ["postgresql-changes-local.public.users"])
    fun customerListener(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Int
    ) {
        val readValue = objectMapper.readValue(message, DebeziumUserMessage::class.java)
        log.info("Received message: $readValue from partition-$partition with offset-$offset")

        webSocketCacheService.allWebSocketSession.forEach {
                it: Map.Entry<String, WebsocketIdentifier> ->
            try {
                it.value.session!!.sendMessage(TextMessage(message))
            } catch (e: Exception) {
                log.error("Exception occurred for sending ping message: ${e.message}")
                webSocketCacheService.deleteSession(key = it.key)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SpringKotlinCdcApplication>(*args)
}

fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger((if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass).name)
}

fun String.split(delimiter: String): Array<String>? = StringUtils.split(this, delimiter)
