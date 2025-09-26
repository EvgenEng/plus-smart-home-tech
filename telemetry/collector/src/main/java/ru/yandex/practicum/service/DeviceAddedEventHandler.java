package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

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
        var hubEvent = new ru.yandex.practicum.model.hub.DeviceAddedEvent();
        setCommonHubFields(hubEvent, event);
        hubEvent.setId(deviceAdded.getId());
        hubEvent.setDeviceType(toDeviceType(deviceAdded.getType()));

        eventService.collectHubEvent(hubEvent);
    }

    private void setCommonHubFields(ru.yandex.practicum.model.hub.HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(java.time.Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }

    private ru.yandex.practicum.model.hub.DeviceType toDeviceType(ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto protoType) {
        return ru.yandex.practicum.model.hub.DeviceType.valueOf(protoType.name());
    }
}
