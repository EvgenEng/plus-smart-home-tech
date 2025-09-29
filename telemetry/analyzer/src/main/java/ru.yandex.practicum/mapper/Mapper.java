package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.model.enums.ActionType;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.model.enums.ConditionType;

@Component
public class Mapper {

    public ConditionType toConditionType(ConditionTypeAvro typeAvro) {
        return switch (typeAvro) {
            case MOTION -> ConditionType.MOTION;
            case SWITCH -> ConditionType.SWITCH;
            case CO2LEVEL -> ConditionType.CO2LEVEL;
            case HUMIDITY -> ConditionType.HUMIDITY;
            case LUMINOSITY -> ConditionType.LUMINOSITY;
            case TEMPERATURE -> ConditionType.TEMPERATURE;
        };
    }

    public ConditionOperation toConditionOperation(ConditionOperationAvro typeAvro) {
        return switch (typeAvro) {
            case EQUALS -> ConditionOperation.EQUALS;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
        };
    }

    public ActionType toActionType(ActionTypeAvro typeAvro) {
        return switch (typeAvro) {
            case INVERSE -> ActionType.INVERSE;
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case SET_VALUE -> ActionType.SET_VALUE;
        };
    }

    public ActionTypeProto toActionTypeProto(ActionType actionType) {
        return switch (actionType) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }
}
