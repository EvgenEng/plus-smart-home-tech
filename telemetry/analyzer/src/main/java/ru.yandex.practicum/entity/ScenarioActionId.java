package ru.yandex.practicum.entity;

import java.util.Objects;

public class ScenarioActionId implements java.io.Serializable {
    private Long scenario;
    private String sensor;
    private Long action;

    public ScenarioActionId() {}

    public ScenarioActionId(Long scenario, String sensor, Long action) {
        this.scenario = scenario;
        this.sensor = sensor;
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioActionId that = (ScenarioActionId) o;
        return Objects.equals(scenario, that.scenario) &&
                Objects.equals(sensor, that.sensor) &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, sensor, action);
    }

    @Override
    public String toString() {
        return "ScenarioActionId{" +
                "scenario=" + scenario +
                ", sensor='" + sensor + '\'' +
                ", action=" + action +
                '}';
    }
}
