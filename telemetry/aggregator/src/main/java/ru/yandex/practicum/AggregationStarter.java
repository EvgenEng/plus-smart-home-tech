package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.AggregatorEventHandler;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private final String TELEMETRY_SENSORS_TOPIC = "telemetry.sensors.v1";
    private final String TELEMETRY_SNAPSHOT_TOPIC = "telemetry.snapshots.v1";

    private final AggregatorEventHandler aggregatorEventHandler;
    private final Producer<String, SpecificRecordBase> producer;
    private final org.apache.kafka.clients.consumer.Consumer<String, SpecificRecordBase> consumer;

    public void start() {
        try {
            consumer.subscribe(List.of(TELEMETRY_SENSORS_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    log.info("Обрабатываем сообщение от устройства: {}", record.key());

                    SensorEventAvro eventAvro = (SensorEventAvro) record.value();
                    Optional<SensorsSnapshotAvro> snapshotAvro = aggregatorEventHandler.updateState(eventAvro);

                    if (snapshotAvro.isPresent()) {
                        log.info("Снапшот обновлен для хаба: {}", eventAvro.getHubId());

                        ProducerRecord<String, SpecificRecordBase> message = new ProducerRecord<>(
                                TELEMETRY_SNAPSHOT_TOPIC,
                                null,
                                eventAvro.getTimestamp().toEpochMilli(),
                                eventAvro.getHubId(),
                                snapshotAvro.get()
                        );
                        producer.send(message);
                    }
                }
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            // Игнорируем - закрываем в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }
}
