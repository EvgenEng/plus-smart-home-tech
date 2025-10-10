package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemperatureSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR_EVENT;
    }

    @Override
    public void handle(SensorEventProto event) {
        log.info("Processing temperature sensor event from device: {}", event.getId());

        var tempSensor = event.getTemperatureSensorEvent();
        var tempEvent = new ru.yandex.practicum.model.sensor.TemperatureSensorEvent();
        setCommonSensorFields(tempEvent, event);
        tempEvent.setTemperatureC(tempSensor.getTemperatureC());
        tempEvent.setTemperatureF(tempSensor.getTemperatureF());

        eventService.collectSensorEvent(tempEvent);
    }

    private void setCommonSensorFields(ru.yandex.practicum.model.sensor.SensorEvent event, SensorEventProto proto) {
        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        event.setTimestamp(java.time.Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }
}
