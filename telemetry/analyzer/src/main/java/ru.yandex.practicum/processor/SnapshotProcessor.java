package ru.yandex.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.config.KafkaConsumerProperties;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.handler.SnapshotHandler;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SnapshotProcessor implements Runnable {
    private final String TELEMETRY_SNAPSHOT_TOPIC = "telemetry.snapshots.v1";

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffset = new HashMap<>();
    private final SnapshotHandler snapshotHandler;
    private final KafkaConsumerProperties properties;

    public SnapshotProcessor(Consumer<String, SensorsSnapshotAvro> consumer,
                             HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient,
                             SnapshotHandler snapshotHandler,
                             KafkaConsumerProperties properties) {
        this.consumer = consumer;
        this.hubRouterClient = hubRouterClient;
        this.snapshotHandler = snapshotHandler;
        this.properties = properties;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(TELEMETRY_SNAPSHOT_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getSensorSnapshot()));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    for (DeviceActionRequest action : snapshotHandler.handle(record.value())) {
                        hubRouterClient.handleDeviceAction(action);
                    }
                    currentOffset.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                }
                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Во время фиксации произошла ошибка. Офсет: {}", offsets, exception);
                    }
                });
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            try {
                consumer.commitSync(currentOffset);
            } finally {
                consumer.close();
            }
        }
    }
}
