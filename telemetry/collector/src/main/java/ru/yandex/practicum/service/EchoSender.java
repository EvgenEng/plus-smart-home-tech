package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.echo.EchoRequest;
import ru.yandex.practicum.grpc.echo.EchoResponse;
import ru.yandex.practicum.grpc.echo.EchoServiceGrpc;

@Slf4j
@Service
public class EchoSender {

    @GrpcClient("echo") // внедрение с параметром "echo" — это отсылка к блоку конфигурации
    EchoServiceGrpc.EchoServiceBlockingStub echoService;

    @Scheduled(initialDelay = 1000, fixedDelay = 1000)
    public void echo() {
        try {
            log.info("echo send");
            // отправка сообщения с помощью готового к использованию метода
            EchoResponse echoResponse = echoService.echo(
                    EchoRequest.newBuilder()
                            .setMessage("Ping from client")
                            .build()
            );
            log.info("echo response received: {}", echoResponse.getMessage());
        } catch (Exception e) {
            log.error("Error sending echo: {}", e.getMessage());
        }
    }
}
