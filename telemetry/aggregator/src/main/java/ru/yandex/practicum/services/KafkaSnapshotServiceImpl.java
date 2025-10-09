package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.KafkaConfig;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSnapshotServiceImpl implements SnapshotService {
    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    @Override
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshotAvro = snapshots.getOrDefault(
                event.getHubId(),
                getNewSensorsSnapshotAvro(event.getHubId())
        );

        SensorStateAvro oldState = snapshotAvro.getSensorsState().get(event.getId());
        if (oldState != null &&
                (oldState.getTimestamp().isAfter(event.getTimestamp()) ||
                        oldState.getData().equals(event.getPayload()))) {
            return Optional.empty();
        }
        SensorStateAvro newState = getNewSensorsSnapshotAvro(event);
        snapshotAvro.getSensorsState().put(event.getId(), newState);
        snapshotAvro.setTimestamp(event.getTimestamp());
        snapshots.put(event.getHubId(), snapshotAvro);
        return Optional.of(snapshotAvro);
    }

    @Override
    public void collectSensorSnapshot(SensorsSnapshotAvro sensorsSnapshotAvro) {
        ProducerRecord<String, SpecificRecordBase> rec = new ProducerRecord<>(
                kafkaConfig.getKafkaProperties().getSensorSnapshotsTopic(),
                null,
                sensorsSnapshotAvro.getTimestamp().toEpochMilli(),
                sensorsSnapshotAvro.getHubId(),
                sensorsSnapshotAvro);

        producer.send(rec, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send sensor snapshot to Kafka for hub: {}",
                        sensorsSnapshotAvro.getHubId(), exception);
            } else {
                log.debug("Successfully sent sensor snapshot to Kafka for hub: {}, partition: {}, offset: {}",
                        sensorsSnapshotAvro.getHubId(), metadata.partition(), metadata.offset());
            }
        });

        producer.flush();
    }

    @Override
    public void close() {
        SnapshotService.super.close();
        if (producer != null) {
            producer.close();
        }
    }

    private SensorsSnapshotAvro getNewSensorsSnapshotAvro(String key) {
        return SensorsSnapshotAvro.newBuilder()
                .setHubId(key)
                .setTimestamp(Instant.now())
                .setSensorsState(new HashMap<>())
                .build();
    }

    private SensorStateAvro getNewSensorsSnapshotAvro(SensorEventAvro event) {
        return SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();
    }
}
