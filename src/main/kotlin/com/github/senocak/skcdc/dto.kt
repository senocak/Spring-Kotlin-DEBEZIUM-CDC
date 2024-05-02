package com.github.senocak.skcdc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.web.socket.WebSocketSession

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class BaseDto

data class WebsocketIdentifier(
    var user: String,
    var session: WebSocketSession? = null
): BaseDto()

data class WsRequestBody(
    var from: String? = null,
    var to: String? = null,
    var content: String? = null,
    var type: String? = null,
    var date: Long? = null
): BaseDto()

data class DebeziumUserMessage<T> (
    var schema  : Schema?,
    var payload : Payload<T>?
)
data class Payload<T> (
    val before: T,
    val after: T,
    val source: Source,
    val op: String,
    val ts_ms: Long,
    val transaction: Any? = null
)
data class Source (
    val version: String,
    val connector: String,
    val name: String,
    val ts_ms: Long,
    val snapshot: String,
    val db: String,
    val sequence: String,
    val schema: String,
    val table: String,
    val txId: Long,
    val lsn: Long,
    val xmin: Any? = null
)

data class Schema (
    val type: String?,
    val fields: List<SchemaField>?,
    val optional: Boolean?,
    val name: String?,
    val version: Long?
)
data class SchemaField (
    val type: String?,
    val fields: List<FieldField>? = null,
    val optional: Boolean?,
    val name: String? = null,
    val field: String?,
    val version: Long? = null
)
data class FieldField (
    val type: String?,
    val optional: Boolean?,
    val name: String? = null,
    val version: Long? = null,
    val field: String?,
    val parameters: Parameters? = null,
    val default: String? = null
)
data class Parameters (
    val allowed: String?
)
