package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaConsumerProperties;
import ru.yandex.practicum.exception.DuplicateException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.handler.HubEventHandler;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private static final String TELEMETRY_HUBS_TOPIC = "telemetry.hubs.v1";

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final HubEventHandler hubEventHandler;
    private final KafkaConsumerProperties properties;

    @Override
    public void run() {
        try {
            hubEventConsumer.subscribe(List.of(TELEMETRY_HUBS_TOPIC));
            Runtime.getRuntime().addShutdownHook(new Thread(hubEventConsumer::wakeup));

            log.info("HubEventProcessor started for topic: {}", TELEMETRY_HUBS_TOPIC);

            while (true) {
                ConsumerRecords<String, HubEventAvro> records =
                        hubEventConsumer.poll(Duration.ofSeconds(properties.getPollDurationSeconds().getHubEvent()));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    try {
                        log.debug("Processing hub event for hub: {}", record.key());
                        hubEventHandler.handle(record.value());
                    } catch (DuplicateException | NotFoundException e) {
                        log.warn("Skipping hub event due to: {}", e.getMessage());
                    } catch (Exception e) {
                        log.error("Error processing hub event: {}", e.getMessage(), e);
                    }
                }

                hubEventConsumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("HubEventProcessor shutdown initiated");
        } catch (Exception e) {
            log.error("Error in HubEventProcessor: {}", e.getMessage(), e);
        } finally {
            hubEventConsumer.close();
            log.info("HubEventProcessor stopped");
        }
    }
}
