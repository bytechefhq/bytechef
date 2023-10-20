create table if not exists task_execution (
  id                   varchar(256) not null primary key,
  parent_id            varchar(256)     null,
  status               varchar(256) not null,
  progress             int not          null,
  job_id               varchar(256) not null,
  create_time          timestamp    not null,
  start_time           timestamp        null,
  end_time             timestamp        null,
  serialized_execution text        not null,
  priority             int          not null,
  task_number          int          not null
);

create index if not exists idx_task_execution_job_id on task_execution (job_id);
