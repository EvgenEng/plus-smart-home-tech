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

-- Создаем функцию для проверки, что связываемые сценарий и датчик работают с одним и тем же хабом
CREATE OR REPLACE FUNCTION check_hub_id()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT hub_id FROM scenarios WHERE id = NEW.scenario_id) != (SELECT hub_id FROM sensors WHERE id = NEW.sensor_id) THEN
        RAISE EXCEPTION 'Hub IDs do not match for scenario_id % and sensor_id %', NEW.scenario_id, NEW.sensor_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Создаем триггер, проверяющий, что «условие» связывает корректные сценарий и датчик
CREATE OR REPLACE TRIGGER tr_bi_scenario_conditions_hub_id_check
    BEFORE INSERT ON scenario_conditions
    FOR EACH ROW
    EXECUTE FUNCTION check_hub_id();

-- Создаем триггер, проверяющий, что «действие» связывает корректные сценарий и датчик
CREATE OR REPLACE TRIGGER tr_bi_scenario_actions_hub_id_check
    BEFORE INSERT ON scenario_actions
    FOR EACH ROW
    EXECUTE FUNCTION check_hub_id();

-- Создаем индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_scenarios_hub_id ON scenarios(hub_id);
CREATE INDEX IF NOT EXISTS idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX IF NOT EXISTS idx_scenario_conditions_scenario_id ON scenario_conditions(scenario_id);
CREATE INDEX IF NOT EXISTS idx_scenario_actions_scenario_id ON scenario_actions(scenario_id);