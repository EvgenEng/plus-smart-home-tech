package ru.yandex.practicum.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.model.hub.*;

@Component
public class HubEventMapper {

    public HubEventAvro toHubEventAvro(HubEvent hubEvent) {
        return new HubEventAvro(
                hubEvent.getHubId(),
                hubEvent.getTimestamp(),
                toHubEventPayloadAvro(hubEvent)
        );
    }

    public SpecificRecordBase toHubEventPayloadAvro(HubEvent hubEvent) {
        switch (hubEvent.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent event = (DeviceAddedEvent) hubEvent;
                return new DeviceAddedEventAvro(
                        event.getId(),
                        toDeviceTypeAvro(event.getDeviceType())
                );
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent event = (DeviceRemovedEvent) hubEvent;
                return new DeviceRemovedEventAvro(
                        event.getId()
                );
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEvent event = (ScenarioAddedEvent) hubEvent;
                return new ScenarioAddedEventAvro(
                        event.getName(),
                        event.getConditions().stream().map(HubEventMapper::toScenarioConditionAvro).toList(),
                        event.getActions().stream().map(HubEventMapper::toDeviceActionAvro).toList()
                );
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent event = (ScenarioRemovedEvent) hubEvent;
                return new ScenarioRemovedEventAvro(
                        event.getName()
                );
            }

            default -> throw new IllegalStateException("Invalid payload");
        }
    }

    public static DeviceTypeAvro toDeviceTypeAvro(DeviceType deviceType) {
        return DeviceTypeAvro.valueOf(deviceType.name());
    }

    public static ConditionTypeAvro toConditionTypeAvro(ConditionType conditionType) {
        return ConditionTypeAvro.valueOf(conditionType.name());
    }

    public static ConditionOperationAvro toConditionOperationAvro(ConditionOperation conditionOperation) {
        return ConditionOperationAvro.valueOf(conditionOperation.name());
    }

    public static ActionTypeAvro toActionTypeAvro(ActionType actionType) {
        return ActionTypeAvro.valueOf(actionType.name());
    }

    public static ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition scenarioCondition) {
        return new ScenarioConditionAvro(
                scenarioCondition.getSensorId(),
                toConditionTypeAvro(scenarioCondition.getType()),
                toConditionOperationAvro(scenarioCondition.getOperation()),
                scenarioCondition.getValue()
        );
    }

    public static DeviceActionAvro toDeviceActionAvro(DeviceAction deviceAction) {
        return new DeviceActionAvro(
                deviceAction.getSensorId(),
                toActionTypeAvro(deviceAction.getType()),
                deviceAction.getValue()
        );
    }
}
