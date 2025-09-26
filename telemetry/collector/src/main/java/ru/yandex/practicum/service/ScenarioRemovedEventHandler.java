package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioRemovedEventHandler implements HubEventHandler {

    private final EventService eventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        log.info("Processing scenario removed event for hub: {}", event.getHubId());

        var scenarioRemoved = event.getScenarioRemoved();
        var hubEvent = new ru.yandex.practicum.model.hub.ScenarioRemovedEvent();

        setCommonHubFields(hubEvent, event);
        hubEvent.setName(scenarioRemoved.getName());

        eventService.collectHubEvent(hubEvent);
    }

    private void setCommonHubFields(ru.yandex.practicum.model.hub.HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(java.time.Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }
}
