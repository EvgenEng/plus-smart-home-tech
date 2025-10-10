package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class LightSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR_EVENT;
    }

    @Override
    public void handle(SensorEventProto event) {
        log.info("Processing light sensor event from device: {}", event.getId());

        var lightSensor = event.getLightSensorEvent();
        var lightEvent = new ru.yandex.practicum.model.sensor.LightSensorEvent();
        setCommonSensorFields(lightEvent, event);
        lightEvent.setLinkQuality(lightSensor.getLinkQuality());
        lightEvent.setLuminosity(lightSensor.getLuminosity());

        eventService.collectSensorEvent(lightEvent);
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
