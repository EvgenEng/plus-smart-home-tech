package ru.yandex.practicum.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaConfigProperties;
import ru.yandex.practicum.services.SensorEventAggregationService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorEventProcessor implements Runnable {

    private final KafkaConsumer<String, SensorEventAvro> sensorEventConsumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> snapshotProducer;
    private final KafkaConfigProperties kafkaProperties;
    private final SensorEventAggregationService aggregationService;

    @Override
    public void run() {
        log.info("Starting sensor event processor...");
        try {
            sensorEventConsumer.subscribe(Collections.singletonList(kafkaProperties.getSensorEventsTopic()));

            while (true) {
                try {
                    ConsumerRecords<String, SensorEventAvro> records =
                            sensorEventConsumer.poll(Duration.ofMillis(kafkaProperties.getConsumeAttemptTimeout()));

                    for (ConsumerRecord<String, SensorEventAvro> record : records) {
                        log.info("Processing sensor event from device: {}", record.key());
                        processSensorEvent(record.value());
                    }
                    if (!records.isEmpty()) {
                        sensorEventConsumer.commitSync();
                    }
                } catch (Exception e) {
                    log.error("Error processing sensor event", e);
                }
            }
        } catch (Exception e) {
            log.error("Error in sensor event processor", e);
        }
    }

    private void processSensorEvent(SensorEventAvro event) {
        try {
            SensorsSnapshotAvro snapshot = aggregationService.processEvent(event);
            if (snapshot != null) {
                ProducerRecord<String, SensorsSnapshotAvro> record = new ProducerRecord<>(
                        "telemetry.snapshots.v1",
                        null,
                        event.getTimestamp().toEpochMilli(),
                        event.getHubId(),
                        snapshot
                );
                snapshotProducer.send(record);
                log.info("Snapshot sent for hub: {}", event.getHubId());
            }
        } catch (Exception e) {
            log.error("Error processing sensor event", e);
        }
    }
}
