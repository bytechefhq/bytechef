drop table if exists job cascade;

create table job (
  id                       varchar(256) not null primary key,
  status                   varchar(256) not null,
  current_task             int          not null,
  workflow_id              varchar(256) not null,
  label                    varchar(256)     null,
  create_time              timestamp    not null,
  start_time               timestamp        null,
  end_time                 timestamp        null,
  priority                 int          not null,
  inputs                   json        not null,
  webhooks                 json        not null,
  outputs                  json        not null,
  parent_task_execution_id varchar(256)
);

create index idx_job_create_time on job (create_time);
create index idx_job_status on job (status);

drop table if exists task_execution cascade;

create table task_execution (
  id                   varchar(256) not null primary key,
  parent_id            varchar(256)     null,
  status               varchar(256) not null,
  progress             int not          null,
  job_id               varchar(256) not null,
  create_time          timestamp    not null,
  start_time           timestamp        null,
  end_time             timestamp        null,
  serialized_execution json        not null,
  priority             int          not null,
  task_number          int          not null
);

create index idx_task_execution_job_id on task_execution (job_id);

drop table if exists context cascade;

create table context (
  id                 varchar(256) not null primary key,
  stack_id           varchar(256) not null,
  create_time        timestamp    not null,
  serialized_context json        not null
);

create index idx_context_stack_id on context (stack_id);

drop table if exists counter cascade;

create table counter (
  id          varchar(256) not null primary key,
  create_time timestamp    not null,
  value       bigint       not null
);
