package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.entity.Condition;
import ru.yandex.practicum.entity.Scenario;
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
import ru.yandex.practicum.mapper.Mapper;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventHandlerImpl implements HubEventHandler {

    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final SensorRepository sensorRepository;
    private final Mapper mapper;

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        String hubId = event.getHubId();

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

        if (scenarioRepository.existsByHubIdAndName(hubId, name)) {
            throw new DuplicateException("Scenario already exists: " + name);
        }

        checkSensorIds(eventAvro, hubId);

        Map<String, Condition> conditions = eventAvro.getConditions().stream()
                .collect(Collectors.toMap(
                        ScenarioConditionAvro::getSensorId,
                        condition -> Condition.builder()
                                .type(mapper.toConditionType(condition.getType()))
                                .operation(mapper.toConditionOperation(condition.getOperation()))
                                .value(extractValue(condition))
                                .build()
                ));

        Map<String, Action> actions = eventAvro.getActions().stream()
                .collect(Collectors.toMap(
                        DeviceActionAvro::getSensorId,
                        action -> Action.builder()
                                .type(mapper.toActionType(action.getType()))
                                .value(action.getValue())
                                .build()
                ));

        conditionRepository.saveAll(conditions.values());
        actionRepository.saveAll(actions.values());

        Map<String, Long> conditionIds = conditions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));

        Map<String, Long> actionIds = actions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));

        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(name)
                .conditionIds(conditionIds)
                .actionIds(actionIds)
                .build();

        scenarioRepository.save(scenario);
        log.info("Scenario added: {} for hub: {}", name, hubId);
    }

    private void deleteScenario(ScenarioRemovedEventAvro eventAvro, String hubId) {
        String name = eventAvro.getName();
        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name)
                .orElseThrow(() -> new NotFoundException("Scenario not found: " + name));

        conditionRepository.deleteAllById(scenario.getConditionIds().values());
        actionRepository.deleteAllById(scenario.getActionIds().values());
        scenarioRepository.delete(scenario);

        log.info("Scenario deleted: {} for hub: {}", name, hubId);
    }

    private void addDevice(DeviceAddedEventAvro eventAvro, String hubId) {
        String sensorId = eventAvro.getId();

        if (sensorRepository.existsById(sensorId)) {
            throw new DuplicateException("Device already exists: " + sensorId);
        }

        Sensor sensor = Sensor.builder()
                .id(sensorId)
                .hubId(hubId)
                .build();

        sensorRepository.save(sensor);
        log.info("Device added: {} for hub: {}", sensorId, hubId);
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
        var conditionSensorIds = eventAvro.getConditions().stream()
                .map(ScenarioConditionAvro::getSensorId)
                .collect(Collectors.toSet());

        var actionSensorIds = eventAvro.getActions().stream()
                .map(DeviceActionAvro::getSensorId)
                .collect(Collectors.toSet());

        if (sensorRepository.findByIdInAndHubId(conditionSensorIds, hubId).size() != conditionSensorIds.size()) {
            throw new NotFoundException("Some condition sensors not found in hub: " + hubId);
        }

        if (sensorRepository.findByIdInAndHubId(actionSensorIds, hubId).size() != actionSensorIds.size()) {
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
