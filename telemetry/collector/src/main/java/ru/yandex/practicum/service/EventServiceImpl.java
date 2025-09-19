package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mapper.HubEventMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;

    private Producer<String, SpecificRecordBase> producer;



    public void collectSensorEvent(SensorEvent event) {
        String topic = "telemetry.sensors.v1";
        initProducer();
        SensorEventAvro message = sensorEventMapper.toSensorEventAvro(event);
        String hubId = message.getHubId().toString();
        Long timestamp = message.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, null, timestamp, hubId, message);
        producer.send(record);
    }

    public void collectHubEvent(HubEvent event) {
        String topic = "telemetry.hubs.v1";
        initProducer();
        HubEventAvro message = hubEventMapper.toHubEventAvro(event);
        String hubId = message.getHubId().toString();
        Long timestamp = message.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, null, timestamp, hubId, message);
        producer.send(record);
    }

    private void initProducer() {
        if (producer == null) {
            Properties config = new Properties();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "ru.yandex.practicum.serializer.GeneralAvroSerializer");

            producer = new KafkaProducer<>(config);
        }
    }
}
