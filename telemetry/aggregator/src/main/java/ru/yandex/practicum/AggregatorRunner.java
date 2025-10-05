package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.processors.SensorEventProcessor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorRunner implements CommandLineRunner {

    private final SensorEventProcessor sensorEventProcessor;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Aggregator application...");

        Thread processorThread = new Thread(sensorEventProcessor, "sensor-event-processor");
        processorThread.setDaemon(true);
        processorThread.start();

        log.info("Sensor event processor started successfully");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.info("Aggregator application interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
