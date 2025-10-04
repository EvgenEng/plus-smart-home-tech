package ru.yandex.practicum.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AggregatorEventHandler {
    Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {

        SensorsSnapshotAvro snapshot = snapshots.getOrDefault(event.getHubId(), createSensorSnapshotAvro(event.getHubId()));

        SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());

        if (oldState != null && (oldState.getTimestamp().isAfter(event.getTimestamp()) || oldState.getData().equals(event.getPayload()))) {
            return Optional.empty();
        } else {
            SensorStateAvro newState = createSensorStateAvro(event);
            snapshot.getSensorsState().put(event.getId(), newState);

            snapshot.setTimestamp(event.getTimestamp());
            snapshots.put(event.getHubId(), snapshot);
            return Optional.of(snapshot);
        }
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
