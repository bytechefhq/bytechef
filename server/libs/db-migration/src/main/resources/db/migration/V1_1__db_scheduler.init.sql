CREATE TABLE IF NOT EXISTS scheduled_tasks (
    task_name             VARCHAR(100),
    task_instance         VARCHAR(100),
    task_data             bytea,
    execution_time        TIMESTAMP,
    picked                BOOLEAN,
    picked_by             VARCHAR(50),
    last_success          TIMESTAMP,
    last_failure          TIMESTAMP,
    consecutive_failures  INT,
    last_heartbeat        TIMESTAMP,
    version               BIGINT,
    PRIMARY KEY (task_name, task_instance)
);
