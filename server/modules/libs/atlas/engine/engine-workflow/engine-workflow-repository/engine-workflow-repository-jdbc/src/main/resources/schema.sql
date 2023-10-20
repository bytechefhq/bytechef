create table if not exists workflow (
id					varchar(256) not null primary key,
content    			text not null,
format     			varchar(256) not null
);
