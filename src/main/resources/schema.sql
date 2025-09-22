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
                                        status varchar,
                                        device varchar
    );


-- уникальность подключения "пациент + провайдер"
create unique index if not exists ux_connections_patient_provider
    on connections (patient_id, provider);



-- ==== 1) Универсальные DAILY-таблицы по датам (aggregated) ====

-- Суточная сводка сна (под Oura daily_sleep, Garmin daily sleep score и т.п.)
create table if not exists sleep_daily (
                                           id                bigserial primary key,
                                           patient_id        bigint      not null,
                                           provider          varchar(32) not null,
                                           day               date        not null,                -- календарный день (локальный)
                                           source_id         varchar(255),
                                           total_sleep_sec   integer,
                                           time_in_bed_sec   integer,
                                           score             integer,
                                           rhr_bpm           smallint,                           -- resting HR
                                           hrv_avg_ms        smallint,
                                           temp_deviation_c  numeric(4,2),                       -- отклонение температуры
                                           resp_rate         numeric(4,2),                       -- дыхание
                                           raw               jsonb,                              -- полный источник
                                           constraint ux_sleep_daily unique (patient_id, provider, day)
);

-- Готовность/восстановление/strain (Oura readiness, WHOOP recovery, Garmin body battery surrogate)
create table if not exists readiness_daily (
                                               id                bigserial primary key,
                                               patient_id        bigint      not null,
                                               provider          varchar(32) not null,
                                               day               date        not null,
                                               source_id         varchar(255),
                                               score             integer,                            -- readiness / recovery score
                                               strain            numeric(5,2),                       -- для WHOOP (optional)
                                               rhr_bpm           smallint,
                                               hrv_avg_ms        smallint,
                                               temp_deviation_c  numeric(4,2),
                                               notes             text,
                                               raw               jsonb,
                                               constraint ux_readiness_daily unique (patient_id, provider, day)
);

-- Суточная активность (шаги, калории, трен.)
create table if not exists activity_daily (
                                              id                bigserial primary key,
                                              patient_id        bigint      not null,
                                              provider          varchar(32) not null,
                                              day               date        not null,
                                              source_id         varchar(255),
                                              steps             integer,
                                              calories_active   integer,
                                              distance_m        integer,
                                              training_load     integer,
                                              raw               jsonb,
                                              constraint ux_activity_daily unique (patient_id, provider, day)
);


-- ==== 2) Сессии (эпизоды) — гибко подходят всем ====

-- Эпизоды сна (WHOOP sleep, Oura sleep, Garmin sleep session)
create table if not exists sleep_sessions (
                                              id                bigserial primary key,
                                              patient_id        bigint      not null,
                                              provider          varchar(32) not null,
                                              source_id         varchar(255) not null,              -- внешний ID
                                              start_time        timestamptz not null,
                                              end_time          timestamptz not null,
                                              duration_sec      integer,
                                              score             integer,
                                              efficiency        smallint,
                                              is_nap            boolean,
                                              hr_avg            smallint,
                                              hr_min            smallint,
                                              hrv_avg_ms        smallint,
                                              raw               jsonb,
                                              constraint ux_sleep_sessions unique (patient_id, provider, source_id)
);

-- Тренировки/активности (WHOOP workout, Garmin activity, Polar training)
create table if not exists activity_sessions (
                                                 id                bigserial primary key,
                                                 patient_id        bigint      not null,
                                                 provider          varchar(32) not null,
                                                 source_id         varchar(255) not null,
                                                 start_time        timestamptz not null,
                                                 end_time          timestamptz not null,
                                                 sport_type        varchar(64),                         -- бег/велосипед/силовая и т.п.
                                                 calories          integer,
                                                 distance_m        integer,
                                                 avg_hr            smallint,
                                                 max_hr            smallint,
                                                 load              integer,                             -- training load/strain аналоги
                                                 raw               jsonb,
                                                 constraint ux_activity_sessions unique (patient_id, provider, source_id)
);


-- ==== 3) Тонкие ряды
-- Пульс по времени (для коротких интервалов)
create table if not exists hr_samples (
                                          id                bigserial primary key,
                                          patient_id        bigint      not null,
                                          provider          varchar(32) not null,
                                          source_id         varchar(255),
                                          ts                timestamptz not null,
                                          bpm               smallint    not null,
                                          constraint ux_hr_samples unique (patient_id, provider, ts)
);

-- SpO2 во времени
create table if not exists spo2_samples (
                                            id                bigserial primary key,
                                            patient_id        bigint      not null,
                                            provider          varchar(32) not null,
                                            source_id         varchar(255),
                                            ts                timestamptz not null,
                                            spo2_pct          numeric(4,1) not null,
                                            constraint ux_spo2_samples unique (patient_id, provider, ts)
);


-- ==== 4) Состояние синхронизации по каждому провайдеру ====

create table if not exists sync_state (
                                          id                  bigserial primary key,
                                          patient_id          bigint      not null,
                                          provider            varchar(32) not null,
                                          last_sleep_day      date,
                                          last_readiness_day  date,
                                          last_activity_day   date,
                                          last_hr_ts          timestamptz,
                                          cursor              jsonb,              -- для whoop/garmin/polar пагинация/курсоры
                                          updated_at          timestamptz not null default now(),
                                          constraint ux_sync_state unique (patient_id, provider)
);
insert into users(email, role, user_id, username, password) values ('dame_un_beso@mail.ru','ADMIN', 1, 'admin', '$2a$12$kjIiKlk/ZFPEVHVRV970we5v2Sh4VIW54kVyZJZxXh6wCwLwx7QG6') on conflict do nothing;
insert into patients(id, user_id, fullname, doctorId)
values (0, (select id from users where email='dame_un_beso@mail.ru'), 'ADMIN', 1)
    on conflict do nothing;
