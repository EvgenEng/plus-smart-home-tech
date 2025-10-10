package ru.yandex.practicum.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfigProperties {
    private String bootstrapServers;
    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();

    @Data
    public static class Topics {
        private String sensorSnapshots;
        private String hubEvents;
    }

    @Data
    public static class Consumer {
        private String snapshotGroupId;
        private String hubEventGroupId;
        private String autoOffsetReset;
        private boolean enableAutoCommit;
        private long consumeTimeout;
        private String keyDeserializer;
        private ValueDeserializers valueDeserializers = new ValueDeserializers();

        @Data
        public static class ValueDeserializers {
            private String sensorsSnapshot;
            private String hubEvent;
        }
    }
}
