create table if not exists users(
    id                  uuid not null primary key,
    email               varchar(255),
    name                varchar(255),
    password            varchar(255),
    email_activated_at  timestamp,
    created_at          timestamp not null,
    updated_at          timestamp not null
);