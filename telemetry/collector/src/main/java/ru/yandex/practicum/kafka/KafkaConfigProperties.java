package ru.yandex.practicum.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka.config")
public class KafkaConfigProperties {
    private String bootstrapServers;
    private String clientIdConfig;
    private String producerKeySerializer;
    private String producerValueSerializer;
    private String sensorEventsTopic;
    private String hubEventsTopic;
}
