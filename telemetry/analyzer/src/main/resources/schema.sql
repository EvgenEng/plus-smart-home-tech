-- Создаем таблицу scenarios
CREATE TABLE IF NOT EXISTS scenarios (
                                         id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         hub_id VARCHAR NOT NULL,
                                         name VARCHAR NOT NULL,
                                         UNIQUE(hub_id, name)
    );

-- Создаем таблицу sensors
CREATE TABLE IF NOT EXISTS sensors (
                                       id VARCHAR PRIMARY KEY,
                                       hub_id VARCHAR NOT NULL
);

-- Создаем таблицу conditions
CREATE TABLE IF NOT EXISTS conditions (
                                          id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                          type VARCHAR NOT NULL,
                                          operation VARCHAR NOT NULL,
                                          value INTEGER
);

-- Создаем таблицу actions
CREATE TABLE IF NOT EXISTS actions (
                                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                       type VARCHAR NOT NULL,
                                       value INTEGER
);

-- Создаем таблицу scenario_conditions, связывающую сценарий, датчик и условие активации сценария
CREATE TABLE IF NOT EXISTS scenario_conditions (
                                                   scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE RESTRICT,
    condition_id BIGINT REFERENCES conditions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, condition_id)
    );

-- Создаем таблицу scenario_actions, связывающую сценарий, датчик и действие, которое нужно выполнить при активации сценария
CREATE TABLE IF NOT EXISTS scenario_actions (
                                                scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR REFERENCES sensors(id) ON DELETE RESTRICT,
    action_id BIGINT REFERENCES actions(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, sensor_id, action_id)
    );

-- Создаем индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_scenarios_hub_id ON scenarios(hub_id);
CREATE INDEX IF NOT EXISTS idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX IF NOT EXISTS idx_scenario_conditions_scenario_id ON scenario_conditions(scenario_id);
CREATE INDEX IF NOT EXISTS idx_scenario_actions_scenario_id ON scenario_actions(scenario_id);