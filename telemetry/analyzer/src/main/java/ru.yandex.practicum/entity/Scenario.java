package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScenarioCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ScenarioAction> actions = new ArrayList<>();

    public void addCondition(ScenarioCondition condition) {
        conditions.add(condition);
        condition.setScenario(this);
    }

    public void addAction(ScenarioAction action) {
        actions.add(action);
        action.setScenario(this);
    }

    public void removeCondition(ScenarioCondition condition) {
        conditions.remove(condition);
        condition.setScenario(null);
    }

    public void removeAction(ScenarioAction action) {
        actions.remove(action);
        action.setScenario(null);
    }
}