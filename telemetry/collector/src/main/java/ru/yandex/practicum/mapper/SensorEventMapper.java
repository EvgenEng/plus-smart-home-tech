package ru.yandex.practicum.mapper;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.model.sensor.LightSensorEvent;
import ru.yandex.practicum.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.model.sensor.SensorEvent;
import ru.yandex.practicum.model.sensor.SwitchSensorEvent;
import ru.yandex.practicum.model.sensor.TemperatureSensorEvent;

@Component
public class SensorEventMapper {

    public SensorEventAvro toSensorEventAvro(SensorEvent sensorEvent) {
        return new SensorEventAvro(
                sensorEvent.getId(),
                sensorEvent.getHubId(),
                sensorEvent.getTimestamp(),
                toSensorEventPayloadAvro(sensorEvent)
        );
    }

    public SpecificRecordBase toSensorEventPayloadAvro(SensorEvent sensorEvent) {
        switch (sensorEvent.getType()) {
            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent event = (ClimateSensorEvent) sensorEvent;
                return new ClimateSensorAvro(
                        event.getTemperatureC(),
                        event.getHumidity(),
                        event.getCo2Level()
                );
            }

            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent event = (LightSensorEvent) sensorEvent;
                return new LightSensorAvro(
                        event.getLinkQuality(),
                        event.getLuminosity()
                );
            }

            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent event = (MotionSensorEvent) sensorEvent;
                return new MotionSensorAvro(
                        event.getLinkQuality(),
                        event.getMotion(),
                        event.getVoltage()
                );
            }

            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent event = (SwitchSensorEvent) sensorEvent;
                return new SwitchSensorAvro(
                        event.getState()
                );
            }

            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent event = (TemperatureSensorEvent) sensorEvent;
                return new TemperatureSensorAvro(
                        event.getId(), event.getHubId(),
                        event.getTimestamp(),
                        event.getTemperatureC(),
                        event.getTemperatureF()
                );
            }

            default -> throw new IllegalStateException("Invalid payload");
        }
    }
}
