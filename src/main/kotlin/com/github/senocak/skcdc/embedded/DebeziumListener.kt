package com.github.senocak.skcdc.embedded

import com.github.senocak.skcdc.logger
import io.debezium.config.Configuration
import io.debezium.data.Envelope.FieldName.AFTER
import io.debezium.data.Envelope.FieldName.BEFORE
import io.debezium.data.Envelope.FieldName.OPERATION
import io.debezium.data.Envelope.Operation
import io.debezium.embedded.Connect
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.RecordChangeEvent
import io.debezium.engine.format.ChangeEventFormat
import org.apache.kafka.connect.data.Struct
import org.apache.kafka.connect.source.SourceRecord
import org.slf4j.Logger
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Service
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Service
class DebeziumListener(configuration: Configuration) : SmartLifecycle {
    private var running: Boolean = false
    private val log: Logger by logger()
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var debeziumEngine: DebeziumEngine<RecordChangeEvent<SourceRecord>>

    init {
        this.debeziumEngine =
            DebeziumEngine.create(ChangeEventFormat.of(Connect::class.java))
                .using(configuration.asProperties())
                .notifying {
                        sourceRecordRecordChangeEvent: RecordChangeEvent<SourceRecord> ->
                    this.handleChangeEvent(sourceRecordRecordChangeEvent)
                }
                .build()
    }

    private fun handleChangeEvent(sourceRecordRecordChangeEvent: RecordChangeEvent<SourceRecord>) {
        val sourceRecord = sourceRecordRecordChangeEvent.record()
        val sourceRecordKey = sourceRecord.key() as Struct
        val sourceRecordValue = sourceRecord.value() as Struct
        val operation = Operation.forCode("${sourceRecordValue.get(OPERATION)}")
        // ALTER TABLE public.mytable REPLICA IDENTITY FULL;
        // https://stackoverflow.com/questions/59799503/postgres-debezium-does-not-publish-the-previous-state-of-a-record
        val before = sourceRecordValue.get(BEFORE)
        val after = sourceRecordValue.get(AFTER)
        log.info("Embedded RecordChangeEvent >>>> sourceRecordKey:$sourceRecordKey, operation:$operation, before: $before, after: $after")
    }

    override fun start(): Unit = executor.execute(debeziumEngine).also { running = true }
    override fun stop(): Unit = debeziumEngine.close().run { running = false }
    override fun isRunning(): Boolean = running
}
