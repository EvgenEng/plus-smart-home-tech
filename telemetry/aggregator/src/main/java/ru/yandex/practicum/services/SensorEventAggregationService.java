package ru.yandex.practicum.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorEventAggregationService {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public SensorsSnapshotAvro processEvent(SensorEventAvro event) {
        SensorsSnapshotAvro snapshot = snapshots.getOrDefault(event.getHubId(),
                createSensorSnapshotAvro(event.getHubId()));

        SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());

        if (oldState != null &&
                (oldState.getTimestamp().isAfter(event.getTimestamp()) ||
                        oldState.getData().equals(event.getPayload()))) {
            return null;
        }

        SensorStateAvro newState = createSensorStateAvro(event);
        snapshot.getSensorsState().put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());

        snapshots.put(event.getHubId(), snapshot);
        return snapshot;
    }

    private SensorsSnapshotAvro createSensorSnapshotAvro(String hubId) {
        return SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(Instant.now())
                .setSensorsState(new HashMap<>())
                .build();
    }

    private SensorStateAvro createSensorStateAvro(SensorEventAvro event) {
        return SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();
    }
}
