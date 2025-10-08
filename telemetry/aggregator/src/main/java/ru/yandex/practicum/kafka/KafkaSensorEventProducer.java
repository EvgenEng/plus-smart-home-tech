package ru.yandex.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSensorEventProducer implements AutoCloseable {

    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConfigProperties kafkaConfig;

    public void sendSnapshot(SensorsSnapshotAvro snapshot) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaConfig.getSensorSnapshotsTopic(),
                null,
                snapshot.getTimestamp().toEpochMilli(),
                snapshot.getHubId(),
                snapshot
        );
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending snapshot to {}: {}", kafkaConfig.getSensorSnapshotsTopic(), exception.getMessage());
            } else if (log.isDebugEnabled()) {
                log.debug("Snapshot sent: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @Override
    public void close() {
        try {
            producer.flush();
        } catch (Exception e) {
            log.warn("Error when flush the producer", e);
        } finally {
            try {
                producer.close();
            } catch (Exception e) {
                log.warn("Error closing producer", e);
            }
        }
    }
}
