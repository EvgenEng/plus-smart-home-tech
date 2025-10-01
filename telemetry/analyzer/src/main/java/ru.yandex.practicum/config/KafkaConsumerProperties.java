package ru.yandex.practicum.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@Getter
@Setter
@ToString
@ConfigurationProperties("analyzer.kafka.consumer")
public class KafkaConsumerProperties {
    private Properties sensorSnapshot;
    private Properties hubEvent;
    private PollDuration pollDurationSeconds;

    @ToString @Getter @Setter
    public static class PollDuration {
        private Long sensorSnapshot;
        private Long hubEvent;
    }
}
