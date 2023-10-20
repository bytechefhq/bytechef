create table if not exists authentication (
    id                       varchar(256) not null primary key,
    name                     varchar(256) not null,
    type                     varchar(256) not null,
    properties               text        not null,
    create_time              timestamp    not null,
    update_time              timestamp    not null
);
