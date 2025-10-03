package ru.yandex.practicum.entity;

import java.util.Objects;

public class ScenarioConditionId implements java.io.Serializable {
    private Long scenario;
    private String sensor;
    private Long condition;

    public ScenarioConditionId() {}

    public ScenarioConditionId(Long scenario, String sensor, Long condition) {
        this.scenario = scenario;
        this.sensor = sensor;
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioConditionId that = (ScenarioConditionId) o;
        return Objects.equals(scenario, that.scenario) &&
                Objects.equals(sensor, that.sensor) &&
                Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, sensor, condition);
    }

    @Override
    public String toString() {
        return "ScenarioConditionId{" +
                "scenario=" + scenario +
                ", sensor='" + sensor + '\'' +
                ", condition=" + condition +
                '}';
    }
}
