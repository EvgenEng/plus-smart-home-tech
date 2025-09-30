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

-- таблица для scenario_conditions
CREATE TABLE IF NOT EXISTS scenario_conditions (
                                                   scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR NOT NULL,
    condition_id BIGINT NOT NULL,
    PRIMARY KEY (scenario_id, sensor_id)
    );

-- таблица для scenario_actions
CREATE TABLE IF NOT EXISTS scenario_actions (
                                                scenario_id BIGINT REFERENCES scenarios(id) ON DELETE CASCADE,
    sensor_id VARCHAR NOT NULL,
    action_id BIGINT NOT NULL,
    PRIMARY KEY (scenario_id, sensor_id)
    );

-- Создаем индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_scenarios_hub_id ON scenarios(hub_id);
CREATE INDEX IF NOT EXISTS idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX IF NOT EXISTS idx_scenario_conditions_scenario_id ON scenario_conditions(scenario_id);
CREATE INDEX IF NOT EXISTS idx_scenario_actions_scenario_id ON scenario_actions(scenario_id);