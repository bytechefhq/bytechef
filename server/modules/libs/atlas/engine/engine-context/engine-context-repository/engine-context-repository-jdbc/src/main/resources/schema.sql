create table if not exists context (
  id                 varchar(256) not null primary key,
  stack_id           varchar(256) not null,
  create_time        timestamp    not null,
  serialized_context text        not null
);

create index if not exists idx_context_stack_id on context (stack_id);
