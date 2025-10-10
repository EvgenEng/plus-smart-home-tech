package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class MotionSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR_EVENT;
    }

    @Override
    public void handle(SensorEventProto event) {
        log.info("Processing motion sensor event from device: {}", event.getId());

        var motionSensor = event.getMotionSensorEvent();
        var motionEvent = new ru.yandex.practicum.model.sensor.MotionSensorEvent();
        setCommonSensorFields(motionEvent, event);
        motionEvent.setLinkQuality(motionSensor.getLinkQuality());
        motionEvent.setMotion(motionSensor.getMotion());
        motionEvent.setVoltage(motionSensor.getVoltage());

        eventService.collectSensorEvent(motionEvent);
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
