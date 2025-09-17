create table if not exists connections (
                                           id bigserial primary key,
                                           patient_id bigint not null,
                                           provider varchar(32) not null,
    access_token text,
    refresh_token text,
    expires_at timestamptz,
    scope text,
    external_user_id varchar(255)
    );

create table if not exists sleep_records (
                                             id bigserial primary key,
                                             patient_id bigint not null,
                                             provider varchar(32) not null,
    start_time timestamptz not null,
    end_time   timestamptz not null,
    score integer,
    stages_json text,
    source_id varchar(255)
    );

-- демо-пациент, чтобы было куда грузить
create table if not exists users (
                                     id bigserial primary key,
                                     email varchar(255) unique,
                                     user_id bigint,
                                     username varchar(255) not null,
                                     password varchar(255) not null,
                                    role varchar(32) not null default 'DOCTOR'
    );

create table if not exists patients (
                                        id bigserial primary key,
                                        user_id bigint,
                                        fullname varchar(255) not null,
                                        doctorId bigint,
                                        status varchar
    );



insert into users(email, role, user_id, username, password) values ('dame_un_beso@mail.ru','ADMIN', 1, 'admin', '$2a$12$kjIiKlk/ZFPEVHVRV970we5v2Sh4VIW54kVyZJZxXh6wCwLwx7QG6') on conflict do nothing;
insert into patients(id, user_id, fullname, doctorId)
values (1, (select id from users where email='dame_un_beso@mail.ru'), 'ADMIN', 1)
    on conflict do nothing;
