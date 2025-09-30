package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.*;
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
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
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

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.info("Found {} scenarios for hub: {}", scenarios.size(), hubId);

        for (Scenario scenario : scenarios) {
            log.info("Checking scenario: {} with conditions: {}", scenario.getName(), scenario.getConditionIds().keySet());

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

        if (!sensorStates.keySet().containsAll(scenario.getConditionIds().keySet())) {
            log.warn("Missing sensors for scenario '{}'", scenario.getName());
            return false;
        }

        for (Map.Entry<String, Long> entry : scenario.getConditionIds().entrySet()) {
            String sensorId = entry.getKey();
            Long conditionId = entry.getValue();

            Condition condition = conditionRepository.findById(conditionId)
                    .orElseThrow(() -> new RuntimeException("Condition not found: " + conditionId));

            SensorStateAvro sensorState = sensorStates.get(sensorId);
            if (!checkCondition(condition, sensorState)) {
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
                if (data instanceof TemperatureSensorAvro temperatureState) {
                    yield checkByConditionOperation(temperatureState.getTemperatureC(), value, operation);
                } else if (data instanceof ClimateSensorAvro climateState) {
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

        for (Map.Entry<String, Long> entry : scenario.getActionIds().entrySet()) {
            String sensorId = entry.getKey();
            Long actionId = entry.getValue();

            Action action = actionRepository.findById(actionId)
                    .orElseThrow(() -> new RuntimeException("Action not found: " + actionId));

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
