package ru.yandex.practicum.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {
    private final KafkaConsumerProperties properties;

    @Bean
    public Consumer<String, SensorsSnapshotAvro> getConsumerSensor() {
        return new KafkaConsumer<>(properties.getSensorSnapshot());
    }

    @Bean
    public Consumer<String, HubEventAvro> getConsumerHub() {
        return new KafkaConsumer<>(properties.getHubEvent());
    }
}
