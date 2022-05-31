create table if not exists context (
  id                 varchar(256) not null primary key,
  stack_id           varchar(256) not null,
  create_time        timestamp    not null,
  serialized_context text        not null
);

create index if not exists idx_context_stack_id on context (stack_id);

create table if not exists counter (
  id          varchar(256) not null primary key,
  create_time timestamp    not null,
  value       bigint       not null
);

create table if not exists job (
  id                       varchar(256) not null primary key,
  status                   varchar(256) not null,
  current_task             int          not null,
  workflow_id              varchar(256) not null,
  label                    varchar(256)     null,
  create_time              timestamp    not null,
  start_time               timestamp        null,
  end_time                 timestamp        null,
  priority                 int          not null,
  inputs                   text        not null,
  webhooks                 text        not null,
  outputs                  text        not null,
  parent_task_execution_id varchar(256)
);

create index if not exists idx_job_create_time on job (create_time);
create index if not exists idx_job_status on job (status);

create table if not exists workflow (
  id					varchar(256) not null primary key,
  content    			text not null,
  format     			varchar(256) not null
);

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
