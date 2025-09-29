package ru.yandex.practicum.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Entity
@Table(name = "scenarios", uniqueConstraints = @UniqueConstraint(columnNames = {"hub_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hub_id")
    private String hubId;

    @Column(name = "name")
    private String name;

    @ElementCollection
    @CollectionTable(name = "scenario_conditions",
            joinColumns = @JoinColumn(name = "scenario_id"))
    @MapKeyColumn(name = "sensor_id")
    @Column(name = "condition_id")
    private Map<String, Long> conditionIds;

    @ElementCollection
    @CollectionTable(name = "scenario_actions",
            joinColumns = @JoinColumn(name = "scenario_id"))
    @MapKeyColumn(name = "sensor_id")
    @Column(name = "action_id")
    private Map<String, Long> actionIds;
}
