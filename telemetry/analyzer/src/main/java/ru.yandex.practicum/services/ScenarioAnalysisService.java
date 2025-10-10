package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.HubRouterClient;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioAnalysisService {

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClient hubRouterClient;

    @Transactional(readOnly = true)
    public void analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        log.info("Analyzing scenarios for hub: {}", hubId);

        // Получаю все сценарии для хаба
        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);

        if (scenarios.isEmpty()) {
            log.info("No scenarios found for hub: {}", hubId);
            return;
        }

        for (Scenario scenario : scenarios) {
            analyzeScenario(scenario, snapshot);
        }
    }

    private void analyzeScenario(Scenario scenario, SensorsSnapshotAvro snapshot) {
        log.info("Analyzing scenario: {} for hub: {}", scenario.getName(), scenario.getHubId());

        log.info("Scenario conditions: {}", scenario.getConditions().size());
        log.info("Scenario actions: {}", scenario.getActions().size());

        boolean allConditionsMet = checkAllConditions(scenario.getConditions(), snapshot);
        if (allConditionsMet) {
            log.info("All conditions met for scenario: {}, executing actions", scenario.getName());
            executeActions(scenario.getActions(), scenario.getName(), scenario.getHubId());
        } else {
            log.info("Conditions not met for scenario: {}", scenario.getName());
        }
    }

    private boolean checkAllConditions(Map<String, Condition> conditions, SensorsSnapshotAvro snapshot) {
        if (conditions.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Condition> entry : conditions.entrySet()) {
            String sensorId = entry.getKey();
            Condition condition = entry.getValue();
            if (!checkCondition(sensorId, condition, snapshot)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCondition(String sensorId, Condition condition, SensorsSnapshotAvro snapshot) {
        Integer sensorValue = extractSensorValue(sensorId, snapshot);

        if (sensorValue == null) {
            log.warn("Sensor {} not found in snapshot or unknown sensor type", sensorId);
            return false;
        }

        boolean result = switch (condition.getOperation()) {
            case EQUALS -> sensorValue.equals(condition.getValue());
            case GREATER_THAN -> sensorValue > condition.getValue();
            case LOWER_THAN -> sensorValue < condition.getValue();
        };

        log.info("Condition check: sensor {} {} {} = {} (actual: {}, expected: {})",
                sensorId, condition.getOperation(), condition.getValue(), result, sensorValue, condition.getValue());
        return result;
    }

    private Integer extractSensorValue(String sensorId, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro sensorState = sensorsState.get(sensorId);

        if (sensorState == null) {
            log.warn("Sensor {} not found in snapshot", sensorId);
            return null;
        }

        Object data = sensorState.getData();
        if (data == null) {
            log.warn("Sensor {} has null data", sensorId);
            return null;
        }

        if (data instanceof ClimateSensorAvro climateSensor) {
            Integer value = climateSensor.getTemperatureC();
            log.info("Extracted temperature value: {} from ClimateSensor for sensor {}", value, sensorId);
            return value;
        } else if (data instanceof LightSensorAvro lightSensor) {
            Integer value = lightSensor.getLuminosity();
            log.info("Extracted luminosity value: {} from LightSensor for sensor {}", value, sensorId);
            return value;
        } else if (data instanceof MotionSensorAvro motionSensor) {
            Integer value = motionSensor.getMotion() ? 1 : 0;
            log.info("Extracted motion value: {} from MotionSensor for sensor {}", value, sensorId);
            return value;
        } else if (data instanceof SwitchSensorAvro switchSensor) {
            Integer value = switchSensor.getState() ? 1 : 0;
            log.info("Extracted switch value: {} from SwitchSensor for sensor {}", value, sensorId);
            return value;
        } else {
            log.warn("Unknown sensor data type: {} for sensor {}",
                    data.getClass().getSimpleName(), sensorId);
            return null;
        }
    }

    private void executeActions(Map<String, Action> actions, String scenarioName, String hubId) {
        for (Map.Entry<String, Action> entry : actions.entrySet()) {
            String sensorId = entry.getKey();
            Action action = entry.getValue();
            log.info("Executing action: device={}, type={}, value={}",
                    sensorId, action.getType(), action.getValue());

            hubRouterClient.sendDeviceAction(hubId, scenarioName, sensorId, action);
        }
    }
}
