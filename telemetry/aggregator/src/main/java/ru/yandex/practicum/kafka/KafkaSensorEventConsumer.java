package ru.yandex.practicum.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaSensorEventConsumer implements AutoCloseable {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaConfigProperties kafkaConfig;

    @Getter
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    public void subscribeToSensorEvents() {
        consumer.subscribe(List.of(kafkaConfig.getSensorEventsTopic()));
    }

    public ConsumerRecords<String, SensorEventAvro> poll(long timeoutMs) {
        return consumer.poll(Duration.ofMillis(timeoutMs));
    }

    public void trackOffset(String topic, int partition, long nextOffset) {
        currentOffsets.put(new TopicPartition(topic, partition), new OffsetAndMetadata(nextOffset));
    }

    public void commitAsyncCurrentOffsets() {
        consumer.commitAsync(currentOffsets, (offsets, exception) -> {
            if (exception != null) {
                log.warn("Error while fixing offsets: {}", offsets, exception);
            }
        });
    }

    public void commitAsync() {
        consumer.commitAsync();
    }

    public void commitSyncCurrentOffsets() {
        consumer.commitSync(currentOffsets);
    }

    public void wakeup() {
        consumer.wakeup();
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (Exception e) {
            log.warn("Error closing consumer", e);
        }
    }
}
