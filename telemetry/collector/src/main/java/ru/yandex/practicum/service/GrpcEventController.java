package ru.yandex.practicum.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.service.CollectorControllerGrpc;
import ru.yandex.practicum.model.hub.HubEvent;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class GrpcEventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final EventService eventService;

    public GrpcEventController(Set<SensorEventHandler> sensorEventHandlers, EventService eventService) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.eventService = eventService;
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received sensor event from hub: {}, device: {}", request.getHubId(), request.getId());

            if (sensorEventHandlers.containsKey(request.getPayloadCase())) {
                sensorEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException("Cannot find handler for event type: " + request.getPayloadCase());
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing sensor event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received hub event from hub: {}", request.getHubId());

            HubEvent hubEvent = convertToHubEvent(request);
            eventService.collectHubEvent(hubEvent);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing hub event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)
            ));
        }
    }

    private HubEvent convertToHubEvent(HubEventProto proto) {
        HubEventProto.PayloadCase payloadCase = proto.getPayloadCase();

        switch (payloadCase) {
            case DEVICE_ADDED:
                var deviceAdded = proto.getDeviceAdded();
                var deviceAddedEvent = new ru.yandex.practicum.model.hub.DeviceAddedEvent();
                setCommonHubFields(deviceAddedEvent, proto);
                deviceAddedEvent.setId(deviceAdded.getId());
                deviceAddedEvent.setDeviceType(convertDeviceType(deviceAdded.getType()));
                return deviceAddedEvent;

            case DEVICE_REMOVED:
                var deviceRemoved = proto.getDeviceRemoved();
                var deviceRemovedEvent = new ru.yandex.practicum.model.hub.DeviceRemovedEvent();
                setCommonHubFields(deviceRemovedEvent, proto);
                deviceRemovedEvent.setId(deviceRemoved.getId());
                return deviceRemovedEvent;

            case SCENARIO_ADDED:
                var scenarioAdded = proto.getScenarioAdded();
                var scenarioAddedEvent = new ru.yandex.practicum.model.hub.ScenarioAddedEvent();
                setCommonHubFields(scenarioAddedEvent, proto);
                scenarioAddedEvent.setName(scenarioAdded.getName());

                // ИНИЦИАЛИЗИРУЕМ СПИСКИ чтобы избежать NullPointerException
                scenarioAddedEvent.setConditions(new java.util.ArrayList<>());
                scenarioAddedEvent.setActions(new java.util.ArrayList<>());

                return scenarioAddedEvent;

            case SCENARIO_REMOVED:
                var scenarioRemoved = proto.getScenarioRemoved();
                var scenarioRemovedEvent = new ru.yandex.practicum.model.hub.ScenarioRemovedEvent();
                setCommonHubFields(scenarioRemovedEvent, proto);
                scenarioRemovedEvent.setName(scenarioRemoved.getName());
                return scenarioRemovedEvent;

            default:
                throw new IllegalArgumentException("Unknown hub event type: " + payloadCase);
        }
    }

    private void setCommonHubFields(HubEvent event, HubEventProto proto) {
        event.setHubId(proto.getHubId());
        event.setTimestamp(java.time.Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos()
        ));
    }

    private ru.yandex.practicum.model.hub.DeviceType convertDeviceType(
            ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto protoType) {
        return ru.yandex.practicum.model.hub.DeviceType.valueOf(protoType.name());
    }
}
