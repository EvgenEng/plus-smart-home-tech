package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.mapper.Mapper;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotHandlerImpl implements SnapshotHandler {
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();
    private final ScenarioRepository scenarioRepository;
    private final Mapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceActionRequest> handle(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Instant snapshotTimestamp = snapshot.getTimestamp();

        if (snapshots.containsKey(hubId)) {
            SensorsSnapshotAvro previousSnapshot = snapshots.get(hubId);
            if (previousSnapshot.getTimestamp().isAfter(snapshotTimestamp)) {
                return List.of();
            }
        }

        snapshots.put(hubId, snapshot);
        List<DeviceActionRequest> actionRequests = new ArrayList<>();

        List<Scenario> scenarios = scenarioRepository.findByHubIdWithDetails(hubId);

        for (Scenario scenario : scenarios) {
            boolean conditionsMet = checkScenarioConditions(scenario, snapshot);

            if (conditionsMet) {
                List<DeviceActionRequest> actions = createActionRequests(scenario, snapshot);
                actionRequests.addAll(actions);
            }
        }

        return actionRequests;
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();

        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            String sensorId = scenarioCondition.getSensor().getId();
            if (!sensorStates.containsKey(sensorId)) {
                return false;
            }
        }

        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            String sensorId = scenarioCondition.getSensor().getId();
            var condition = scenarioCondition.getCondition();

            SensorStateAvro sensorState = sensorStates.get(sensorId);
            boolean conditionResult = checkCondition(condition, sensorState);

            if (!conditionResult) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCondition(ru.yandex.practicum.entity.Condition condition, SensorStateAvro sensorState) {
        Object data = sensorState.getData();
        Integer value = condition.getValue();
        ConditionOperation operation = condition.getOperation();

        return switch (condition.getType()) {
            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temperatureState) {
                    yield checkByConditionOperation(temperatureState.getTemperatureC(), value, operation);
                }
                if (data instanceof ClimateSensorAvro climateState) {
                    yield checkByConditionOperation(climateState.getTemperatureC(), value, operation);
                }
                throw new IllegalArgumentException("Unsupported sensor type for temperature");
            }
            case LUMINOSITY -> {
                LightSensorAvro lightSensorState = (LightSensorAvro) data;
                yield checkByConditionOperation(lightSensorState.getLuminosity(), value, operation);
            }
            case HUMIDITY -> {
                ClimateSensorAvro climateSensorState = (ClimateSensorAvro) data;
                yield checkByConditionOperation(climateSensorState.getHumidity(), value, operation);
            }
            case CO2LEVEL -> {
                ClimateSensorAvro climateSensorState = (ClimateSensorAvro) data;
                yield checkByConditionOperation(climateSensorState.getCo2Level(), value, operation);
            }
            case SWITCH -> {
                SwitchSensorAvro switchSensorState = (SwitchSensorAvro) data;
                yield (switchSensorState.getState() ? 1 : 0) == value;
            }
            case MOTION -> {
                MotionSensorAvro motionSensorState = (MotionSensorAvro) data;
                yield (motionSensorState.getMotion() ? 1 : 0) == value;
            }
        };
    }

    private boolean checkByConditionOperation(int currentValue, int conditionValue, ConditionOperation operation) {
        return switch (operation) {
            case EQUALS -> currentValue == conditionValue;
            case GREATER_THAN -> currentValue > conditionValue;
            case LOWER_THAN -> currentValue < conditionValue;
        };
    }

    private List<DeviceActionRequest> createActionRequests(Scenario scenario, SensorsSnapshotAvro snapshot) {
        List<DeviceActionRequest> requests = new ArrayList<>();

        for (ScenarioAction scenarioAction : scenario.getActions()) {
            String sensorId = scenarioAction.getSensor().getId();
            var action = scenarioAction.getAction();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(scenario.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(DeviceActionProto.newBuilder()
                            .setSensorId(sensorId)
                            .setType(mapper.toActionTypeProto(action.getType()))
                            .setValue(action.getValue())
                            .build())
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(snapshot.getTimestamp().getEpochSecond())
                            .setNanos(snapshot.getTimestamp().getNano())
                            .build())
                    .build();

            requests.add(request);
        }

        return requests;
    }
}
