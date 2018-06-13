drop table if exists job;

create table job (
  id varchar(256) not null primary key,
  status varchar(256) not null,
  current_task int not null,
  pipeline_id varchar(256) not null,
  label varchar(256),
  create_time timestamp not null,
  start_time timestamp,
  end_time timestamp,
  tags text not null,
  priority int not null,
  inputs text not null,
  webhooks text not null,
  outputs text not null
);

CREATE INDEX ON job (create_time);
CREATE INDEX ON job (status);

drop table if exists task_execution;

create table task_execution (
  id varchar(256) not null primary key,
  parent_id varchar(256) null,
  status varchar(256) not null,
  job_id varchar(256) not null,
  create_time timestamp not null,
  start_time timestamp,
  end_time timestamp,
  serialized_execution text not null,
  priority int not null,
  task_number int not null
);

CREATE INDEX ON task_execution (job_id);

drop table if exists context;

create table context (
  id varchar(256) not null primary key,
  stack_id varchar(256) not null,
  create_time timestamp not null,
  serialized_context text not null
);

CREATE INDEX ON context (stack_id);

drop table if exists counter;

create table counter (
  id varchar(256) not null primary key,
  create_time timestamp not null,
  value bigint not null
);