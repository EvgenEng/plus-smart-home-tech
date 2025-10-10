package ru.yandex.practicum.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaConfigProperties;
import ru.yandex.practicum.services.HubEventHandler;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final KafkaConfigProperties kafkaProperties;
    private final List<HubEventHandler> hubEventHandlers;

    @Override
    public void run() {
        log.info("Starting hub event processor...");

        try {
            hubEventConsumer.subscribe(Collections.singletonList(kafkaProperties.getTopics().getHubEvents()));

            while (true) {
                try {
                    ConsumerRecords<String, HubEventAvro> records =
                            hubEventConsumer.poll(Duration.ofMillis(kafkaProperties.getConsumer().getConsumeTimeout()));

                    for (ConsumerRecord<String, HubEventAvro> record : records) {
                        log.info("Received hub event: {}", record.key());
                        processHubEvent(record.value());
                    }

                    if (!records.isEmpty()) {
                        hubEventConsumer.commitSync();
                    }
                } catch (WakeupException e) {
                    log.info("Hub event processor received wakeup signal, shutting down...");
                    break;
                } catch (Exception e) {
                    log.error("Error processing hub event", e);
                }
            }
        } catch (Exception e) {
            log.error("Error in hub event processor", e);
        } finally {
            try {
                hubEventConsumer.close();
                log.info("Hub event consumer closed successfully");
            } catch (Exception e) {
                log.warn("Error closing hub event consumer", e);
            }
        }
    }

    private void processHubEvent(HubEventAvro hubEvent) {
        log.info("Processing hub event with payload type: {}",
                hubEvent.getPayload().getClass().getSimpleName());

        SpecificRecordBase payload = (SpecificRecordBase) hubEvent.getPayload();

        if (payload instanceof DeviceAddedEventAvro) {
            DeviceAddedEventAvro deviceAddedEvent = (DeviceAddedEventAvro) payload;
            handleWithEvent(deviceAddedEvent, hubEvent);
        } else if (payload instanceof DeviceRemovedEventAvro) {
            DeviceRemovedEventAvro deviceRemovedEvent = (DeviceRemovedEventAvro) payload;
            handleWithEvent(deviceRemovedEvent, hubEvent);
        } else if (payload instanceof ScenarioAddedEventAvro) {
            ScenarioAddedEventAvro scenarioAddedEvent = (ScenarioAddedEventAvro) payload;
            handleWithEvent(scenarioAddedEvent, hubEvent);
        } else if (payload instanceof ScenarioRemovedEventAvro) {
            ScenarioRemovedEventAvro scenarioRemovedEvent = (ScenarioRemovedEventAvro) payload;
            handleWithEvent(scenarioRemovedEvent, hubEvent);
        } else {
            log.warn("Unknown payload type: {}", payload.getClass().getSimpleName());
        }
    }

    private void handleWithEvent(SpecificRecordBase event, HubEventAvro hubEvent) {
        String payloadType = event.getClass().getSimpleName();
        boolean handled = false;

        for (HubEventHandler handler : hubEventHandlers) {
            if (handler.getTypeOfPayload().equals(payloadType)) {
                handler.handle(hubEvent);
                handled = true;
                break;
            }
        }

        if (!handled) {
            log.warn("No handler found for payload type: {}", payloadType);
        }
    }
}
