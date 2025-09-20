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
import ru.yandex.practicum.model.hub.ActionType;
import ru.yandex.practicum.model.hub.ConditionOperation;
import ru.yandex.practicum.model.hub.ConditionType;
import ru.yandex.practicum.model.hub.DeviceAction;
import ru.yandex.practicum.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.model.hub.DeviceType;
import ru.yandex.practicum.model.hub.HubEvent;
import ru.yandex.practicum.model.hub.ScenarioAddedEvent;
import ru.yandex.practicum.model.hub.ScenarioCondition;
import ru.yandex.practicum.model.hub.ScenarioRemovedEvent;

import java.time.Instant;

@Component
public class HubEventMapper {

    public HubEventAvro toHubEventAvro(HubEvent hubEvent) {
        HubEventAvro avro = new HubEventAvro();
        avro.setHubId(hubEvent.getHubId());
        avro.setTimestamp(Instant.ofEpochSecond(hubEvent.getTimestamp().toEpochMilli()));
        avro.setPayload(toHubEventPayloadAvro(hubEvent));
        return avro;
    }

    private SpecificRecordBase toHubEventPayloadAvro(HubEvent hubEvent) {
        switch (hubEvent.getType()) {
            case DEVICE_ADDED -> {
                DeviceAddedEvent event = (DeviceAddedEvent) hubEvent;
                DeviceAddedEventAvro avro = new DeviceAddedEventAvro();
                avro.setId(event.getId());
                avro.setType(toDeviceTypeAvro(event.getDeviceType()));
                return avro;
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent event = (DeviceRemovedEvent) hubEvent;
                DeviceRemovedEventAvro avro = new DeviceRemovedEventAvro();
                avro.setId(event.getId());
                return avro;
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEvent event = (ScenarioAddedEvent) hubEvent;
                ScenarioAddedEventAvro avro = new ScenarioAddedEventAvro();
                avro.setName(event.getName());
                avro.setConditions(event.getConditions().stream()
                        .map(this::toScenarioConditionAvro)
                        .collect(java.util.stream.Collectors.toList()));
                avro.setActions(event.getActions().stream()
                        .map(this::toDeviceActionAvro)
                        .collect(java.util.stream.Collectors.toList()));
                return avro;
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent event = (ScenarioRemovedEvent) hubEvent;
                ScenarioRemovedEventAvro avro = new ScenarioRemovedEventAvro();
                avro.setName(event.getName());
                return avro;
            }

            default -> throw new IllegalStateException("Invalid payload");
        }
    }

    private DeviceTypeAvro toDeviceTypeAvro(DeviceType deviceType) {
        return DeviceTypeAvro.valueOf(deviceType.name());
    }

    private ConditionTypeAvro toConditionTypeAvro(ConditionType conditionType) {
        return ConditionTypeAvro.valueOf(conditionType.name());
    }

    private ConditionOperationAvro toConditionOperationAvro(ConditionOperation conditionOperation) {
        return ConditionOperationAvro.valueOf(conditionOperation.name());
    }

    private ActionTypeAvro toActionTypeAvro(ActionType actionType) {
        return ActionTypeAvro.valueOf(actionType.name());
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition scenarioCondition) {
        ScenarioConditionAvro avro = new ScenarioConditionAvro();
        avro.setSensorId(scenarioCondition.getSensorId());
        avro.setType(toConditionTypeAvro(scenarioCondition.getType()));
        avro.setOperation(toConditionOperationAvro(scenarioCondition.getOperation()));

        Object value = scenarioCondition.getValue();
        if (value == null) {
            avro.setValue(null);
        } else if (value instanceof Boolean) {
            avro.setValue((Boolean) value);
        } else if (value instanceof Number) {
            avro.setValue(((Number) value).intValue());
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass());
        }

        return avro;
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceAction deviceAction) {
        DeviceActionAvro avro = new DeviceActionAvro();
        avro.setSensorId(deviceAction.getSensorId());
        avro.setType(toActionTypeAvro(deviceAction.getType()));
        avro.setValue(deviceAction.getValue());
        return avro;
    }
}
