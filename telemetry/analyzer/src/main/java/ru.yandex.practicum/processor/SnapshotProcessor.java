package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {
    private final String TELEMETRY_SNAPSHOT_TOPIC = "telemetry.snapshots.v1";

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffset = new HashMap<>();
    private final SnapshotHandler snapshotHandler;
    private final KafkaConsumerProperties properties;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(TELEMETRY_SNAPSHOT_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        consumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getSensorSnapshot()));
                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    log.info("Обработка снапшота для хаба: {}", record.key());
                    List<DeviceActionRequest> actions = snapshotHandler.handle(record.value());
                    log.info("Найдено {} действий для отправки в Hub Router", actions.size());

                    actions.forEach(action -> {
                        try {
                            hubRouterStub.handleDeviceAction(action);
                            log.info("Успешно отправлено действие в Hub Router: hub={}, scenario={}, sensor={}, type={}, value={}",
                                    action.getHubId(), action.getScenarioName(), action.getAction().getSensorId(),
                                    action.getAction().getType(), action.getAction().getValue());
                        } catch (Exception e) {
                            log.error("Ошибка отправки действия в Hub Router: {}", e.getMessage(), e);
                        }
                    });

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
            log.error("Ошибка во время обработки снапшотов", e);
        } finally {
            try {
                consumer.commitSync(currentOffset);
            } finally {
                consumer.close();
            }
        }
    }
}
