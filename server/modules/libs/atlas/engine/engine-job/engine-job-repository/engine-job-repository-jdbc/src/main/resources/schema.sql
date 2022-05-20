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
