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
import ru.yandex.practicum.entity.Sensor;
import ru.yandex.practicum.exception.DuplicateException;
import ru.yandex.practicum.exception.NotFoundException;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventHandlerImpl implements HubEventHandler {

    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final SensorRepository sensorRepository;

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        String hubId = event.getHubId();

        log.info("=== PROCESSING HUB EVENT ===");
        log.info("Hub: {}, Type: {}", event.getHubId(), payload.getClass().getSimpleName());

        if (payload instanceof ScenarioAddedEventAvro eventAvro) {
            addScenario(eventAvro, hubId);
        } else if (payload instanceof ScenarioRemovedEventAvro eventAvro) {
            deleteScenario(eventAvro, hubId);
        } else if (payload instanceof DeviceAddedEventAvro eventAvro) {
            addDevice(eventAvro, hubId);
        } else if (payload instanceof DeviceRemovedEventAvro eventAvro) {
            deleteDevice(eventAvro, hubId);
        } else {
            log.warn("Unknown payload type: {}", payload.getClass().getSimpleName());
        }
    }

    private void addScenario(ScenarioAddedEventAvro eventAvro, String hubId) {
        String name = eventAvro.getName();

        log.info("=== ADDING SCENARIO ===");
        log.info("Hub: {}, Scenario: {}", hubId, name);

        if (scenarioRepository.existsByHubIdAndName(hubId, name)) {
            throw new DuplicateException("Scenario already exists: " + name);
        }

        checkSensorIds(eventAvro, hubId);

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(name)
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .build();

        for (ScenarioConditionAvro conditionAvro : eventAvro.getConditions()) {
            Condition condition = Condition.builder()
                    .type(conditionAvro.getType())
                    .operation(conditionAvro.getOperation())
                    .value(extractValue(conditionAvro))
                    .build();

            condition = conditionRepository.save(condition);

            Sensor sensor = sensorRepository.findById(conditionAvro.getSensorId())
                    .orElseThrow(() -> new NotFoundException("Sensor not found: " + conditionAvro.getSensorId()));

            ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                    .scenario(scenario)
                    .sensor(sensor)
                    .condition(condition)
                    .build();

            scenario.addCondition(scenarioCondition);
        }

        for (DeviceActionAvro actionAvro : eventAvro.getActions()) {
            Action action = Action.builder()
                    .type(actionAvro.getType())
                    .value(actionAvro.getValue())
                    .build();

            action = actionRepository.save(action);

            Sensor sensor = sensorRepository.findById(actionAvro.getSensorId())
                    .orElseThrow(() -> new NotFoundException("Sensor not found: " + actionAvro.getSensorId()));

            ScenarioAction scenarioAction = ScenarioAction.builder()
                    .scenario(scenario)
                    .sensor(sensor)
                    .action(action)
                    .build();

            scenario.addAction(scenarioAction);
        }

        scenario = scenarioRepository.save(scenario);

        log.info("✅ Scenario added successfully: {} for hub: {}", name, hubId);
        log.info("Conditions count: {}", scenario.getConditions().size());
        log.info("Actions count: {}", scenario.getActions().size());
    }

    private void deleteScenario(ScenarioRemovedEventAvro eventAvro, String hubId) {
        String name = eventAvro.getName();
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name)
                .orElseThrow(() -> new NotFoundException("Scenario not found: " + name));

        List<Long> conditionIds = scenario.getConditions().stream()
                .map(sc -> sc.getCondition().getId())
                .collect(Collectors.toList());

        List<Long> actionIds = scenario.getActions().stream()
                .map(sa -> sa.getAction().getId())
                .collect(Collectors.toList());

        conditionRepository.deleteAllById(conditionIds);
        actionRepository.deleteAllById(actionIds);
        scenarioRepository.delete(scenario);

        log.info("Scenario deleted: {} for hub: {}", name, hubId);
    }

    private void addDevice(DeviceAddedEventAvro eventAvro, String hubId) {
        String sensorId = eventAvro.getId();

        log.info("Adding device: {} for hub: {}", sensorId, hubId);

        if (sensorRepository.existsById(sensorId)) {
            throw new DuplicateException("Device already exists: " + sensorId);
        }

        Sensor sensor = Sensor.builder()
                .id(sensorId)
                .hubId(hubId)
                .build();

        sensorRepository.save(sensor);
        log.info("✅ Device added: {} for hub: {}", sensorId, hubId);
    }

    private void deleteDevice(DeviceRemovedEventAvro eventAvro, String hubId) {
        String sensorId = eventAvro.getId();

        if (!sensorRepository.existsByIdAndHubId(sensorId, hubId)) {
            throw new NotFoundException("Device not found: " + sensorId);
        }

        sensorRepository.deleteById(sensorId);
        log.info("Device deleted: {} for hub: {}", sensorId, hubId);
    }

    private void checkSensorIds(ScenarioAddedEventAvro eventAvro, String hubId) {
        Set<String> conditionSensorIds = eventAvro.getConditions().stream()
                .map(ScenarioConditionAvro::getSensorId)
                .collect(Collectors.toSet());

        Set<String> actionSensorIds = eventAvro.getActions().stream()
                .map(DeviceActionAvro::getSensorId)
                .collect(Collectors.toSet());

        log.info("Condition sensors: {}", conditionSensorIds);
        log.info("Action sensors: {}", actionSensorIds);

        List<Sensor> existingConditionSensors = sensorRepository.findByIdInAndHubId(conditionSensorIds, hubId);
        List<Sensor> existingActionSensors = sensorRepository.findByIdInAndHubId(actionSensorIds, hubId);

        log.info("Existing condition sensors: {}", existingConditionSensors.stream().map(Sensor::getId).collect(Collectors.toList()));
        log.info("Existing action sensors: {}", existingActionSensors.stream().map(Sensor::getId).collect(Collectors.toList()));

        if (existingConditionSensors.size() != conditionSensorIds.size()) {
            throw new NotFoundException("Some condition sensors not found in hub: " + hubId);
        }

        if (existingActionSensors.size() != actionSensorIds.size()) {
            throw new NotFoundException("Some action sensors not found in hub: " + hubId);
        }
    }

    private Integer extractValue(ScenarioConditionAvro conditionAvro) {
        Object value = conditionAvro.getValue();
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        return 0;
    }
}
