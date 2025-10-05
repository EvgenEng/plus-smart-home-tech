package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.processor.HubEventProcessor;
import ru.yandex.practicum.processor.SnapshotProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {

    private final HubEventProcessor hubEventProcessor;
    private final SnapshotProcessor snapshotProcessor;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Analyzer application...");

        Thread hubEventsThread = new Thread(hubEventProcessor, "hub-event-processor");
        hubEventsThread.setDaemon(true);
        hubEventsThread.start();

        Thread snapshotThread = new Thread(snapshotProcessor, "snapshot-processor");
        snapshotThread.setDaemon(true);
        snapshotThread.start();

        log.info("Kafka processors started successfully");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("Analyzer application interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
