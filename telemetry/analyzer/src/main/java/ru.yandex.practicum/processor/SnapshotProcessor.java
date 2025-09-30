package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConsumerProperties;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.handler.SnapshotHandler;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {

    private static final String TELEMETRY_SNAPSHOT_TOPIC = "telemetry.snapshots.v1";

    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final SnapshotHandler snapshotHandler;
    private final KafkaConsumerProperties properties;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Override
    public void run() {
        try {
            snapshotConsumer.subscribe(List.of(TELEMETRY_SNAPSHOT_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(snapshotConsumer::wakeup));

            log.info("SnapshotProcessor started for topic: {}", TELEMETRY_SNAPSHOT_TOPIC);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        snapshotConsumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getSensorSnapshot()));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        log.info("Processing snapshot for hub: {}", record.key());
                        List<DeviceActionRequest> actions = snapshotHandler.handle(record.value());

                        log.info("Generated {} actions for hub: {}", actions.size(), record.key());

                        for (DeviceActionRequest action : actions) {
                            try {
                                log.info("Sending gRPC action for scenario: {}, sensor: {}",
                                        action.getScenarioName(), action.getAction().getSensorId());

                                var response = hubRouterClient.handleDeviceAction(action);
                                log.info("✅ Successfully executed action for scenario: {} on hub: {}",
                                        action.getScenarioName(), action.getHubId());
                            } catch (Exception e) {
                                log.error("❌ Failed to execute action for scenario: {}: {}",
                                        action.getScenarioName(), e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing snapshot: {}", e.getMessage(), e);
                    }
                }

                snapshotConsumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("SnapshotProcessor shutdown initiated");
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor: {}", e.getMessage(), e);
        } finally {
            snapshotConsumer.close();
            log.info("SnapshotProcessor stopped");
        }
    }
}
