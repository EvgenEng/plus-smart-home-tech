package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.mapper.Mapper;
import ru.yandex.practicum.model.enums.ConditionOperation;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
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

        log.info("=== PROCESSING SNAPSHOT for hub: {} ===", hubId);
        log.info("Snapshot sensors: {}", snapshot.getSensorsState().keySet());

        if (snapshots.containsKey(hubId)) {
            SensorsSnapshotAvro previousSnapshot = snapshots.get(hubId);
            if (previousSnapshot.getTimestamp().isAfter(snapshotTimestamp)) {
                log.debug("Skipping outdated snapshot for hub: {}", hubId);
                return List.of();
            }
        }

        snapshots.put(hubId, snapshot);
        List<DeviceActionRequest> actionRequests = new ArrayList<>();

        List<Scenario> scenarios = scenarioRepository.findByHubIdWithDetails(hubId);
        log.info("Found {} scenarios for hub: {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            log.info("Checking scenario: {} with {} conditions and {} actions",
                    scenario.getName(), scenario.getConditions().size(), scenario.getActions().size());

            boolean conditionsMet = checkScenarioConditions(scenario, snapshot);

            log.info("Scenario '{}' conditions met: {}", scenario.getName(), conditionsMet);

            if (conditionsMet) {
                List<DeviceActionRequest> actions = createActionRequests(scenario, snapshot);
                log.info("Created {} actions for scenario '{}'", actions.size(), scenario.getName());
                actionRequests.addAll(actions);
            }
        }

        log.info("Total actions generated for hub {}: {}", hubId, actionRequests.size());
        return actionRequests;
    }

    private boolean checkScenarioConditions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();

        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            String sensorId = scenarioCondition.getSensor().getId();
            if (!sensorStates.containsKey(sensorId)) {
                log.warn("Sensor {} not found in snapshot for scenario '{}'", sensorId, scenario.getName());
                return false;
            }
        }

        for (ScenarioCondition scenarioCondition : scenario.getConditions()) {
            String sensorId = scenarioCondition.getSensor().getId();
            Condition condition = scenarioCondition.getCondition();

            SensorStateAvro sensorState = sensorStates.get(sensorId);
            boolean conditionResult = checkCondition(condition, sensorState);

            if (!conditionResult) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCondition(Condition condition, SensorStateAvro sensorState) {
        Object data = sensorState.getData();
        Integer value = condition.getValue();
        ConditionOperation operation = condition.getOperation();

        return switch (condition.getType()) {
            case TEMPERATURE -> {
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro temperatureState) {
                    yield checkByConditionOperation(temperatureState.getTemperatureC(), value, operation);
                }
                if (data instanceof ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climateState) {
                    yield checkByConditionOperation(climateState.getTemperatureC(), value, operation);
                }
                throw new IllegalArgumentException("Unsupported sensor type for temperature");
            }
            case LUMINOSITY -> {
                ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro lightSensorState = (ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro) data;
                yield checkByConditionOperation(lightSensorState.getLuminosity(), value, operation);
            }
            case HUMIDITY -> {
                ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climateSensorState = (ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro) data;
                yield checkByConditionOperation(climateSensorState.getHumidity(), value, operation);
            }
            case CO2LEVEL -> {
                ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro climateSensorState = (ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro) data;
                yield checkByConditionOperation(climateSensorState.getCo2Level(), value, operation);
            }
            case SWITCH -> {
                ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro switchSensorState = (ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro) data;
                yield (switchSensorState.getState() ? 1 : 0) == value;
            }
            case MOTION -> {
                ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro motionSensorState = (ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro) data;
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
            Action action = scenarioAction.getAction();

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
            log.info("Created action for sensor: {}, type: {}, value: {}", sensorId, action.getType(), action.getValue());
        }

        return requests;
    }
}
