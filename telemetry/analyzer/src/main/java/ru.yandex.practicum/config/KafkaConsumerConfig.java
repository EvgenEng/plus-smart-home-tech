package ru.yandex.practicum.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public class KafkaConsumerConfig {

    private final KafkaConsumerProperties properties;

    public KafkaConsumerConfig(KafkaConsumerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> sensorSnapshotConsumer() {
        Properties props = new Properties();
        props.putAll(properties.getSensorSnapshot());
        return new KafkaConsumer<>(props);
    }

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer() {
        Properties props = new Properties();
        props.putAll(properties.getHubEvent());
        return new KafkaConsumer<>(props);
    }
}
