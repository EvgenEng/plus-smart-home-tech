package ru.yandex.practicum.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.config")
public class KafkaConfigProperties {
    private String bootstrapServers;

    private String producerClientIdConfig;
    private String producerKeySerializer;
    private String producerValueSerializer;

    private String consumerGroupId;
    private String consumerClientIdConfig;
    private String consumerKeyDeserializer;
    private String consumerValueDeserializer;
    private long consumeAttemptTimeout;

    private String sensorEventsTopic;
    private String sensorSnapshotsTopic;
}
