package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mapper.HubEventMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final Producer<String, SpecificRecordBase> producer;

    public void collectSensorEvent(SensorEvent event) {
        String topic = "telemetry.sensors.v1";
        SensorEventAvro message = sensorEventMapper.toSensorEventAvro(event);
        String hubId = message.getHubId();
        Long timestamp = message.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, null, timestamp, hubId, message);
        producer.send(record);
    }

    public void collectHubEvent(HubEvent event) {
        String topic = "telemetry.hubs.v1";
        HubEventAvro message = hubEventMapper.toHubEventAvro(event);
        String hubId = message.getHubId();
        Long timestamp = message.getTimestamp().toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, null, timestamp, hubId, message);
        producer.send(record);
    }
}
