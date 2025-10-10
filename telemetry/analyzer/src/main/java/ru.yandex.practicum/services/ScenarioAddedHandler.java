package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.Sensor;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioAddedHandler implements HubEventHandler {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;

    @Override
    @Transactional
    public void handle(HubEventAvro hubEventAvro) {

        ScenarioAddedEventAvro scenarioAddedEvent = (ScenarioAddedEventAvro) hubEventAvro.getPayload();
        Scenario scenario = findOrCreateScenario(hubEventAvro, scenarioAddedEvent);
        processActionsAndConditions(hubEventAvro, scenarioAddedEvent, scenario);

    }

    public Scenario findOrCreateScenario(HubEventAvro hubEvent, ScenarioAddedEventAvro scenarioAddedEvent) {
        return scenarioRepository.findByHubIdAndName(hubEvent.getHubId(), scenarioAddedEvent.getName())
                .orElseGet(() -> createNewScenario(hubEvent, scenarioAddedEvent));
    }

    public Scenario createNewScenario(HubEventAvro hubEvent, ScenarioAddedEventAvro scenarioAddedEvent) {
        Scenario newScenario = Scenario.builder()
                .hubId(hubEvent.getHubId())
                .name(scenarioAddedEvent.getName())
                .build();
        return scenarioRepository.save(newScenario);
    }

    private void processActionsAndConditions(HubEventAvro hubEvent, ScenarioAddedEventAvro scenarioAddedEvent,
                                             Scenario scenario) {
        Set<String> allSensorIds = new HashSet<>();
        allSensorIds.addAll(scenarioAddedEvent.getActions().stream()
                .map(DeviceActionAvro::getSensorId)
                .collect(Collectors.toSet()));
        allSensorIds.addAll(scenarioAddedEvent.getConditions().stream()
                .map(ScenarioConditionAvro::getSensorId)
                .collect(Collectors.toSet()));

        List<Sensor> sensors = sensorRepository.findByIdInAndHubId(new ArrayList<>(allSensorIds), hubEvent.getHubId());
        Map<String, Sensor> sensorMap = sensors.stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        scenario.getConditions().clear();
        scenario.getActions().clear();

        for (DeviceActionAvro actionAvro : scenarioAddedEvent.getActions()) {
            Sensor sensor = sensorMap.get(actionAvro.getSensorId());
            if (sensor != null) {
                Action action = Action.builder()
                        .type(actionAvro.getType())
                        .value(actionAvro.getValue())
                        .build();
                action = actionRepository.save(action);
                scenario.getActions().put(sensor.getId(), action);
            } else {
                log.warn("Sensor {} not found for action in scenario {}", actionAvro.getSensorId(), scenario.getName());
            }
        }

        for (ScenarioConditionAvro conditionAvro : scenarioAddedEvent.getConditions()) {
            Sensor sensor = sensorMap.get(conditionAvro.getSensorId());
            if (sensor != null) {
                Integer conditionValue = extractConditionValue(conditionAvro.getValue());
                Condition condition = Condition.builder()
                        .type(conditionAvro.getType())
                        .operation(conditionAvro.getOperation())
                        .value(conditionValue)
                        .build();
                condition = conditionRepository.save(condition);
                scenario.getConditions().put(sensor.getId(), condition);
            } else {
                log.warn("Sensor {} not found for condition in scenario {}", conditionAvro.getSensorId(),
                        scenario.getName());
            }
        }

        scenarioRepository.save(scenario);
    }

    @Override
    public String getTypeOfPayload() {
        return ScenarioAddedEventAvro.class.getSimpleName();
    }

    private Integer extractConditionValue(Object value) {
        if (value instanceof Integer intValue) {
            return intValue;
        } else if (value instanceof Boolean boolValue) {
            return boolValue ? 1 : 0;
        }
        return null;
    }
}
