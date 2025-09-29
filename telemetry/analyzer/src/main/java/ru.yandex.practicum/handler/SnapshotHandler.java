package ru.yandex.practicum.handler;

import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.List;

public interface SnapshotHandler {
    List<DeviceActionRequest> handle(SensorsSnapshotAvro snapshot);
}
