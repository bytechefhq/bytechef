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

create table task_execution (
  id varchar(256) not null primary key,
  parent_id varchar(256),
  status varchar(256) not null,
  job_id varchar(256) not null,
  create_time timestamp not null,
  start_time timestamp,
  end_time timestamp,
  serialized_execution text not null,
  priority int not null,
  task_number int not null
);

create table context (
  id varchar(256) not null primary key,
  stack_id varchar(256) not null,
  create_time timestamp not null,
  serialized_context text not null
);

create table counter (
  id varchar(256) not null primary key,
  create_time timestamp not null,
  value bigint not null
);