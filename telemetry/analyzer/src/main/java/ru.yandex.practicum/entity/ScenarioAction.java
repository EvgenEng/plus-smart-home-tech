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
@Table(name = "scenario_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ScenarioActionId.class)
public class ScenarioAction {
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
    @JoinColumn(name = "action_id")
    private Action action;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioAction that = (ScenarioAction) o;
        return Objects.equals(scenario != null ? scenario.getId() : null,
                that.scenario != null ? that.scenario.getId() : null) &&
                Objects.equals(sensor != null ? sensor.getId() : null,
                        that.sensor != null ? that.sensor.getId() : null) &&
                Objects.equals(action != null ? action.getId() : null,
                        that.action != null ? that.action.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                scenario != null ? scenario.getId() : null,
                sensor != null ? sensor.getId() : null,
                action != null ? action.getId() : null
        );
    }
}
