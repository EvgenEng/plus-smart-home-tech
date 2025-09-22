package ru.yandex.practicum.model.hub;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.validation.AllowedConditionValue;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScenarioCondition {

    String sensorId;

    ConditionType type;

    ConditionOperation operation;

    @AllowedConditionValue
    Object value;
}
