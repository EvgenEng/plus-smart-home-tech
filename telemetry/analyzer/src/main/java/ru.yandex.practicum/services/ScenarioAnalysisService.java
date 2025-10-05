package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.HubRouterClient;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
import ru.yandex.practicum.entity.ScenarioAction;
import ru.yandex.practicum.entity.ScenarioCondition;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

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
        List<Scenario> scenarios = scenarioRepository.findByHubIdWithDetails(hubId);

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

    private boolean checkAllConditions(List<ScenarioCondition> conditions, SensorsSnapshotAvro snapshot) {
        if (conditions.isEmpty()) {
            return false;
        }
        for (ScenarioCondition scenarioCondition : conditions) {
            String sensorId = scenarioCondition.getSensor().getId();
            Condition condition = scenarioCondition.getCondition();
            if (!checkCondition(sensorId, condition, snapshot)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCondition(String sensorId, Condition condition, SensorsSnapshotAvro snapshot) {
        Integer sensorValue = findSensorValue(sensorId, snapshot);

        if (sensorValue == null) {
            log.warn("Sensor {} not found in snapshot", sensorId);
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

    private Integer findSensorValue(String sensorId, SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro sensorState = sensorsState.get(sensorId);
        if (sensorState == null) {
            log.warn("Sensor {} not found in snapshot", sensorId);
            return null;
        }
        return extractSensorValue(sensorState);
    }

    private Integer extractSensorValue(SensorStateAvro sensorState) {
        Object data = sensorState.getData();
        Integer value = null;

        // Заменяем pattern matching на instanceof для Java 17
        if (data instanceof ClimateSensorAvro) {
            ClimateSensorAvro climateSensor = (ClimateSensorAvro) data;
            value = climateSensor.getTemperatureC();
            log.info("Extracted temperature value: {} from ClimateSensor", value);
        } else if (data instanceof LightSensorAvro) {
            LightSensorAvro lightSensor = (LightSensorAvro) data;
            value = lightSensor.getLuminosity();
            log.info("Extracted luminosity value: {} from LightSensor", value);
        } else if (data instanceof MotionSensorAvro) {
            MotionSensorAvro motionSensor = (MotionSensorAvro) data;
            value = motionSensor.getMotion() ? 1 : 0;
            log.info("Extracted motion value: {} from MotionSensor", value);
        } else if (data instanceof SwitchSensorAvro) {
            SwitchSensorAvro switchSensor = (SwitchSensorAvro) data;
            value = switchSensor.getState() ? 1 : 0;
            log.info("Extracted switch value: {} from SwitchSensor", value);
        } else if (data instanceof TemperatureSensorAvro) {
            TemperatureSensorAvro temperatureSensor = (TemperatureSensorAvro) data;
            value = temperatureSensor.getTemperatureC();
            log.info("Extracted temperature value: {} from TemperatureSensor", value);
        } else {
            log.warn("Unknown sensor data type: {} for sensor",
                    data != null ? data.getClass().getSimpleName() : "null");
        }
        return value;
    }

    private void executeActions(List<ScenarioAction> actions, String scenarioName, String hubId) {
        for (ScenarioAction scenarioAction : actions) {
            String sensorId = scenarioAction.getSensor().getId();
            Action action = scenarioAction.getAction();
            log.info("Executing action: device={}, type={}, value={}",
                    sensorId, action.getType(), action.getValue());

            hubRouterClient.sendDeviceAction(hubId, scenarioName, sensorId, action);
        }
    }
}
