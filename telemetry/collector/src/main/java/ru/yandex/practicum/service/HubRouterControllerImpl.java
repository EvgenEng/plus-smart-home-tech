package ru.yandex.practicum.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Slf4j
@GrpcService
public class HubRouterControllerImpl extends HubRouterControllerGrpc.HubRouterControllerImplBase {

    @Override
    public void handleDeviceAction(DeviceActionRequest request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Received device action for hub: {}, sensor: {}, scenario: {}, type: {}, value: {}",
                    request.getHubId(),
                    request.getAction().getSensorId(),
                    request.getScenarioName(),
                    request.getAction().getType(),
                    request.getAction().getValue());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing device action", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)
            ));
        }
    }
}
