package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;

import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements HubEventHandler {

    private final EventService eventService;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        log.info("Processing scenario added event for hub: {}", event.getHubId());

        var scenarioAdded = event.getScenarioAdded();
        var hubEvent = new ru.yandex.practicum.model.hub.ScenarioAddedEvent();

        setCommonHubFields(hubEvent, event);
        hubEvent.setName(scenarioAdded.getName());

        // Конвертация условий
        hubEvent.setConditions(scenarioAdded.getConditionList().stream()
                .map(this::toScenarioCondition)
                .collect(Collectors.toList()));

        // Конвертация действий
        hubEvent.setActions(scenarioAdded.getActionList().stream()
                .map(this::toDeviceAction)
                .collect(Collectors.toList()));

        eventService.collectHubEvent(hubEvent);
    }

    private void setCommonHubFields(ru.yandex.practicum.model.hub.HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(java.time.Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }

    private ru.yandex.practicum.model.hub.ScenarioCondition toScenarioCondition(
            ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto proto) {
        var condition = new ru.yandex.practicum.model.hub.ScenarioCondition();
        condition.setSensorId(proto.getSensorId());
        condition.setType(toConditionType(proto.getType()));
        condition.setOperation(toConditionOperation(proto.getOperation()));

        // Обработка значения условия
        switch (proto.getValueCase()) {
            case BOOL_VALUE -> condition.setValue(proto.getBoolValue());
            case INT_VALUE -> condition.setValue(proto.getIntValue());
            case VALUE_NOT_SET -> condition.setValue(null);
        }

        return condition;
    }

    private ru.yandex.practicum.model.hub.DeviceAction toDeviceAction(
            ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto proto) {
        var action = new ru.yandex.practicum.model.hub.DeviceAction();
        action.setSensorId(proto.getSensorId());
        action.setType(toActionType(proto.getType()));

        if (proto.hasValue()) {
            action.setValue(proto.getValue());
        }

        return action;
    }

    private ru.yandex.practicum.model.hub.ConditionType toConditionType(
            ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto protoType) {
        return ru.yandex.practicum.model.hub.ConditionType.valueOf(protoType.name());
    }

    private ru.yandex.practicum.model.hub.ConditionOperation toConditionOperation(
            ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto protoOp) {
        return ru.yandex.practicum.model.hub.ConditionOperation.valueOf(protoOp.name());
    }

    private ru.yandex.practicum.model.hub.ActionType toActionType(
            ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto protoType) {
        return ru.yandex.practicum.model.hub.ActionType.valueOf(protoType.name());
    }
}
