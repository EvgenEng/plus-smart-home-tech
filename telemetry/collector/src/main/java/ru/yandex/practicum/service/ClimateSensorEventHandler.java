package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClimateSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        log.info("Processing climate sensor event from device: {}", event.getId());

        var climateSensor = event.getClimateSensor();
        var climateEvent = new ru.yandex.practicum.model.sensor.ClimateSensorEvent();
        setCommonSensorFields(climateEvent, event);
        climateEvent.setTemperatureC(climateSensor.getTemperatureC());
        climateEvent.setHumidity(climateSensor.getHumidity());
        climateEvent.setCo2Level(climateSensor.getCo2Level());

        eventService.collectSensorEvent(climateEvent);
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
