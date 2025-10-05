package ru.yandex.practicum.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka.config")
public class KafkaConfigProperties {
    private String bootstrapServers;
    private String sensorEventsTopic;
    private String consumerGroupId;
    private String consumerClientIdConfig;
    private String consumerKeyDeserializer;
    private String consumerValueDeserializer;
    private long consumeAttemptTimeout;
    private String producerClientIdConfig;
    private String producerKeySerializer;
    private String producerValueSerializer;
}
