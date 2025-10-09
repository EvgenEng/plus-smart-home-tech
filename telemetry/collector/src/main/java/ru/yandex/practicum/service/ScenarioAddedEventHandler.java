package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.hub.ScenarioAddedEvent;
import ru.yandex.practicum.model.hub.ScenarioCondition;
import ru.yandex.practicum.model.hub.DeviceAction;
import ru.yandex.practicum.model.hub.ConditionType;
import ru.yandex.practicum.model.hub.ConditionOperation;
import ru.yandex.practicum.model.hub.ActionType;

import java.time.Instant;
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
        var hubEvent = new ScenarioAddedEvent();

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

    private void setCommonHubFields(HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }

    private ScenarioCondition toScenarioCondition(ScenarioConditionProto proto) {
        var condition = new ScenarioCondition();
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

    private DeviceAction toDeviceAction(DeviceActionProto proto) {
        var action = new DeviceAction();
        action.setSensorId(proto.getSensorId());
        action.setType(toActionType(proto.getType()));

        if (proto.hasValue()) {
            action.setValue(proto.getValue());
        }

        return action;
    }

    private ConditionType toConditionType(ConditionTypeProto protoType) {
        return ConditionType.valueOf(protoType.name());
    }

    private ConditionOperation toConditionOperation(ConditionOperationProto protoOp) {
        return ConditionOperation.valueOf(protoOp.name());
    }

    private ActionType toActionType(ActionTypeProto protoType) {
        return ActionType.valueOf(protoType.name());
    }
}
