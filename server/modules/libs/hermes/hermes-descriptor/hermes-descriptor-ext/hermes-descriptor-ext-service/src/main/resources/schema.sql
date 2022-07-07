create table if not exists descriptor_ext_handler (
    name                     varchar(256) not null primary key,
    versions                 varchar(256) not null,
    authentication_exists    boolean  not null,
    type                     varchar(256) not null,
    properties               text        not null,
    create_time              timestamp    not null,
    update_time              timestamp    not null
);
