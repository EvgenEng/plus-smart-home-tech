package ru.yandex.practicum.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ScenarioConditionId.class)
public class ScenarioCondition {
    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @Id
    @ManyToOne
    @JoinColumn(name = "condition_id")
    private Condition condition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioCondition that = (ScenarioCondition) o;
        return Objects.equals(scenario != null ? scenario.getId() : null,
                that.scenario != null ? that.scenario.getId() : null) &&
                Objects.equals(sensor != null ? sensor.getId() : null,
                        that.sensor != null ? that.sensor.getId() : null) &&
                Objects.equals(condition != null ? condition.getId() : null,
                        that.condition != null ? that.condition.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                scenario != null ? scenario.getId() : null,
                sensor != null ? sensor.getId() : null,
                condition != null ? condition.getId() : null
        );
    }
}
