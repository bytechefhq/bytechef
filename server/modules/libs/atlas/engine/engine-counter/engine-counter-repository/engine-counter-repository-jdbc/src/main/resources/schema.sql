create table if not exists counter (
   id          varchar(256) not null primary key,
   create_time timestamp    not null,
   value       bigint       not null
);
