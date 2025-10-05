package ru.yandex.practicum.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.entity.Action;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubRouterClient {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    public void sendDeviceAction(String hubId, String scenarioName, String sensorId, Action action) {
        try {
            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(ActionTypeProto.valueOf(action.getType().toString()));
            if (action.getValue() != null) {
                actionBuilder.setValue(action.getValue());
            }
            DeviceActionProto actionProto = actionBuilder.build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(actionProto)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            log.info("Sending device action to hub {}: device={}, type={}, value={}",
                    hubId, sensorId, action.getType(), action.getValue());

            hubRouterStub.handleDeviceAction(request);

            log.info("Device action sent successfully");
        } catch (Exception e) {
            log.error("Error sending command to device", e);
        }
    }
}
