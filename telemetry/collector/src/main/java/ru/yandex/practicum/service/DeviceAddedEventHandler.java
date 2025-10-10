package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.model.hub.DeviceType;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {

    private final EventService eventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        log.info("Processing device added event for hub: {}", event.getHubId());

        var deviceAdded = event.getDeviceAdded();
        var hubEvent = new DeviceAddedEvent();
        setCommonHubFields(hubEvent, event);
        hubEvent.setId(deviceAdded.getId());
        hubEvent.setDeviceType(toDeviceType(deviceAdded.getType()));

        eventService.collectHubEvent(hubEvent);
    }

    private void setCommonHubFields(HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }

    private DeviceType toDeviceType(DeviceTypeProto protoType) {
        return DeviceType.valueOf(protoType.name());
    }
}
