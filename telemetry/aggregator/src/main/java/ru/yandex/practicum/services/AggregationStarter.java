package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaConfigProperties;
import ru.yandex.practicum.kafka.KafkaSensorEventConsumer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationStarter {

    private final KafkaSensorEventConsumer consumer;
    private final SnapshotService snapshotService;
    private final KafkaConfigProperties kafkaConfig;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribeToSensorEvents();
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records =
                        consumer.poll(kafkaConfig.getConsumeAttemptTimeout());

                int processedCount = 0;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    handleRecord(record);
                    consumer.trackOffset(record.topic(), record.partition(), record.offset() + 1);
                    processedCount++;
                }

                if (processedCount > 0) {
                    consumer.commitAsyncCurrentOffsets();
                    log.debug("Committed offsets for {} processed records", processedCount);
                }
            }

        } catch (WakeupException ignores) {
            // Игнорируем, это нормальное завершение
        } catch (Exception e) {
            log.error("An error occurred while processing events from sensors", e);
        } finally {
            try {
                consumer.commitSyncCurrentOffsets();
            } catch (Exception e) {
                log.warn("Error during final commitSync", e);
            } finally {
                log.info("Closing the consumer");
                consumer.close();
                log.info("Closing the producer");
                snapshotService.close();
            }
        }
    }

    private void handleRecord(ConsumerRecord<String, SensorEventAvro> consumerRecord) {
        Optional<SensorsSnapshotAvro> snapshotAvro = snapshotService.updateState(consumerRecord.value());
        snapshotAvro.ifPresent(snapshotService::collectSensorSnapshot);
    }
}
