package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class SwitchSensorEventHandler implements SensorEventHandler {

    private final EventService eventService;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        log.info("Processing switch sensor event from device: {}", event.getId());

        var switchSensor = event.getSwitchSensor();
        var switchEvent = new ru.yandex.practicum.model.sensor.SwitchSensorEvent();
        setCommonSensorFields(switchEvent, event);
        switchEvent.setState(switchSensor.getState());

        eventService.collectSensorEvent(switchEvent);
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
