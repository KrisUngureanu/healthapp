create table if not exists connections
(
    id               bigserial primary key,
    patient_id       bigint      not null,
    provider         varchar(32) not null,
    access_token     text,
    refresh_token    text,
    expires_at       timestamptz,
    scope            text,
    external_user_id varchar(255)
);

create table if not exists sleep_records
(
    id          bigserial primary key,
    patient_id  bigint      not null,
    provider    varchar(32) not null,
    start_time  timestamptz not null,
    end_time    timestamptz not null,
    score       integer,
    stages_json text,
    source_id   varchar(255)
);

-- демо-пациент, чтобы было куда грузить
create table if not exists users
(
    id       bigserial primary key,
    email    varchar(255) unique,
    user_id  bigint,
    username varchar(255) not null,
    password varchar(255) not null,
    role     varchar(32)  not null default 'DOCTOR'
);

create table if not exists patients
(
    id                    bigserial primary key,
    user_id               bigint,
    fullname              varchar(255) not null,
    doctorId              bigint,
    status                varchar,
    device                varchar,
    birth_date            date,
    gender                varchar(32),
    json_data             text,

    height_cm             numeric(6, 2),
    weight_kg             numeric(6, 2),
    rhr_bpm               smallint,
    hrv_rmssd_ms          smallint,
    body_measurement_json text
);


-- уникальность подключения "пациент + провайдер"
create unique index if not exists ux_connections_patient_provider
    on connections (patient_id, provider);



-- ==== 1) Универсальные DAILY-таблицы по датам (aggregated) ====

-- Суточная сводка сна (под Oura daily_sleep, Garmin daily sleep score и т.п.)
create table if not exists sleep_daily
(
    id               bigserial primary key,
    patient_id       bigint      not null,
    provider         varchar(32) not null,
    day              date        not null, -- календарный день (локальный)
    source_id        varchar(255),
    total_sleep_sec  integer,
    time_in_bed_sec  integer,
    score            integer,
    rhr_bpm          smallint,             -- resting HR
    hrv_avg_ms       smallint,
    temp_deviation_c numeric(4, 2),        -- отклонение температуры
    resp_rate        numeric(4, 2),        -- дыхание
    raw              jsonb,                -- полный источник
    constraint ux_sleep_daily unique (patient_id, provider, day)
);

-- Готовность/восстановление/strain (Oura readiness, WHOOP recovery, Garmin body battery surrogate)
create table if not exists readiness_daily
(
    id               bigserial primary key,
    patient_id       bigint      not null,
    provider         varchar(32) not null,
    day              date        not null,
    source_id        varchar(255),
    score            integer,       -- readiness / recovery score
    strain           numeric(5, 2), -- для WHOOP (optional)
    rhr_bpm          smallint,
    hrv_avg_ms       smallint,
    temp_deviation_c numeric(4, 2),
    notes            text,
    raw              jsonb,
    constraint ux_readiness_daily unique (patient_id, provider, day)
);

-- Суточная активность (шаги, калории, трен.)
create table if not exists activity_daily
(
    id              bigserial primary key,
    patient_id      bigint      not null,
    provider        varchar(32) not null,
    day             date        not null,
    source_id       varchar(255),
    steps           integer,
    calories_active integer,
    distance_m      integer,
    training_load   integer,
    raw             jsonb,
    constraint ux_activity_daily unique (patient_id, provider, day)
);


-- ==== 2) Сессии (эпизоды) — гибко подходят всем ====

-- Эпизоды сна (WHOOP sleep, Oura sleep, Garmin sleep session)
create table if not exists sleep_sessions
(
    id           bigserial primary key,
    patient_id   bigint       not null,
    provider     varchar(32)  not null,
    source_id    varchar(255) not null, -- внешний ID
    start_time   timestamptz  not null,
    end_time     timestamptz  not null,
    duration_sec integer,
    score        integer,
    efficiency   smallint,
    is_nap       boolean,
    hr_avg       smallint,
    hr_min       smallint,
    hrv_avg_ms   smallint,
    raw          jsonb,
    constraint ux_sleep_sessions unique (patient_id, provider, source_id)
);

-- Тренировки/активности (WHOOP workout, Garmin activity, Polar training)
create table if not exists activity_sessions
(
    id         bigserial primary key,
    patient_id bigint       not null,
    provider   varchar(32)  not null,
    source_id  varchar(255) not null,
    start_time timestamptz  not null,
    end_time   timestamptz  not null,
    sport_type varchar(64), -- бег/велосипед/силовая и т.п.
    calories   integer,
    distance_m integer,
    avg_hr     smallint,
    max_hr     smallint,
    load       integer,     -- training load/strain аналоги
    raw        jsonb,
    constraint ux_activity_sessions unique (patient_id, provider, source_id)
);


-- ==== 3) Тонкие ряды
-- Пульс по времени (для коротких интервалов)
create table if not exists hr_samples
(
    id         bigserial primary key,
    patient_id bigint      not null,
    provider   varchar(32) not null,
    source_id  varchar(255),
    ts         timestamptz not null,
    bpm        smallint    not null,
    constraint ux_hr_samples unique (patient_id, provider, ts)
);

-- SpO2 во времени
create table if not exists spo2_samples
(
    id         bigserial primary key,
    patient_id bigint        not null,
    provider   varchar(32)   not null,
    source_id  varchar(255),
    ts         timestamptz   not null,
    spo2_pct   numeric(4, 1) not null,
    constraint ux_spo2_samples unique (patient_id, provider, ts)
);


-- ==== 4) Состояние синхронизации по каждому провайдеру ====

create table if not exists sync_state
(
    id                 bigserial primary key,
    patient_id         bigint      not null,
    provider           varchar(32) not null,
    last_sleep_day     date,
    last_readiness_day date,
    last_activity_day  date,
    last_hr_ts         timestamptz,
    cursor             jsonb, -- для whoop/garmin/polar пагинация/курсоры
    updated_at         timestamptz not null default now(),
    constraint ux_sync_state unique (patient_id, provider)
);


create table if not exists whoop_cycle
(
    id                 bigserial primary key,
    record_id          integer not null,
    userid             integer not null,
    patient_id         bigint,
    created_at         timestamptz,
    updated_at         timestamptz,
    start              timestamptz,
    enddate            timestamptz,
    timezone_offset    varchar(16),
    score_state        varchar(32),
    strain             real,
    kilojoule          real,
    average_heart_rate integer,
    max_heart_rate     integer

);


create table if not exists whoop_sleep
(
    id                               bigserial primary key,
    record_id                        varchar not null,
    userid                           integer not null,
    patient_id                       bigint,
    cycle_id                         integer, -- references whoop_cycle.record_id
    created_at                       timestamptz,
    updated_at                       timestamptz,
    start                            timestamptz,
    enddate                          timestamptz,
    timezone_offset                  varchar(16),
    score_state                      varchar(32),
    nap                              boolean,
    total_in_bed_time_milli          integer,
    total_awake_time_milli           integer,
    total_no_data_time_milli         integer,
    total_light_sleep_time_milli     integer,
    total_slow_wave_sleep_time_milli integer,
    total_rem_sleep_time_milli       integer,
    sleep_cycle_count                integer,
    disturbance_count                integer,
    baseline_milli                   integer,
    need_from_sleep_debt_milli       integer,
    need_from_recent_strain_milli    integer,
    need_from_recent_nap_milli       integer,
    respiratory_rate                 real,
    sleep_performance_percentage     real,
    sleep_consistency_percentage     real,
    sleep_efficiency_percentage      real

);


create table if not exists whoop_recovery
(
    id                 bigserial primary key,
    record_id          integer not null,
    cycle_id           integer, -- references whoop_cycle.record_id
    sleep_id           varchar, -- references whoop_sleep.record_id
    userid             integer not null,
    patient_id         bigint,
    created_at         timestamptz,
    updated_at         timestamptz,
    score_state        varchar(32),
    user_calibrating   boolean,
    recovery_score     real,
    resting_heart_rate real,
    hrv_rmssd_milli    real,
    spo2_percentage    real,
    skin_temp_celsius  real

);


create table if not exists whoop_workout
(
    id                    bigserial primary key,
    record_id             varchar not null,
    userid                integer not null,
    patient_id            bigint,
    created_at            timestamptz,
    updated_at            timestamptz,
    start                 timestamptz,
    enddate               timestamptz,
    timezone_offset       varchar(16),
    sport_name            varchar(255),
    score_state           varchar(32),
    strain                real,
    average_heart_rate    integer,
    max_heart_rate        integer,
    kilojoule             real,
    percent_recorded      real,
    distance_meter        real,
    altitude_gain_meter   real,
    altitude_change_meter real

);
-- OuraActivity
create table if not exists oura_activity
(
    id                          bigserial primary key,
    record_id                   varchar,
    active_calories             integer,
    average_met_minutes         real,
    meet_daily_targets          integer,
    move_every_hour             integer,
    recovery_time               integer,
    stay_active                 integer,
    training_frequency          integer,
    training_volume             integer,

    day                         varchar(16),
    equivalent_walking_distance integer,
    high_activity_met_minutes   integer,
    high_activity_time          integer,
    inactivity_alerts           integer,
    low_activity_met_minutes    integer,
    low_activity_time           integer,
    medium_activity_met_minutes integer,
    medium_activity_time        integer,

    meters_to_target            integer,
    non_wear_time               integer,
    resting_time                integer,
    score                       integer,
    sedentary_met_minutes       integer,
    sedentary_time              integer,
    steps                       integer,
    target_calories             integer,
    target_meters               integer,

    total_calories              integer,
    time_record                 timestamptz,
    patient_id                  bigint
);

-- OuraDaylySleep
create table if not exists oura_daylysleep
(
    id          bigserial primary key,
    record_id   varchar,
    deep_sleep  integer,
    efficiency  integer,
    latency     integer,
    rem_sleep   integer,
    restfulness integer,
    timing      integer,
    total_sleep integer,
    day         varchar(16),
    score       integer,
    time_record timestamptz,
    patient_id  bigint
);

-- OuraHeartRate
create table if not exists oura_heartrate
(
    id          bigserial primary key,
    time_record timestamptz,
    bpm         integer,
    source      varchar,
    patient_id  bigint
);

-- OuraReadiness
create table if not exists oura_readiness
(
    id                    bigserial primary key,
    record_id             varchar,
    activity_balance      integer,
    body_temperature      integer,
    hrv_balance           integer,
    previous_day_activity integer,
    previous_night        integer,
    recovery_index        integer,
    resting_heart_rate    integer,
    sleep_balance         integer,
    sleep_regularity      integer,
    day                   varchar(16),
    score                 integer,
    time_record           timestamptz,
    patient_id            bigint
);

-- OuraSleep
create table if not exists oura_sleep
(
    id                    bigserial primary key,
    record_id             varchar,
    average_breath        real,
    average_heart_rate    real,
    average_hrv           integer,
    awake_time            integer,
    bedtime_end           timestamptz,
    bedtime_start         timestamptz,
    day                   varchar(16),
    deep_sleep_duration   integer,
    efficiency            integer,
    latency               integer,
    light_sleep_duration  integer,
    lowest_heart_rate     integer,
    activity_balance      integer,
    body_temperature      integer,
    hrv_balance           integer,
    previous_day_activity integer,
    previous_night        integer,
    recovery_index        integer,
    resting_heart_rate    integer,
    sleep_balance         integer,
    score                 integer,
    temperature_deviation real,
    readiness_score_delta integer,
    rem_sleep_duration    integer,
    restless_periods      integer,
    time_in_bed           integer,
    total_sleep_duration  integer,
    type                  varchar,
    patient_id            bigint
);

-- OuraSpo
create table if not exists oura_spo
(
    id                          bigserial primary key,
    record_id                   varchar,
    breathing_disturbance_index integer,
    day                         varchar(16),
    spo2_percentage             real,
    patient_id                  bigint
);


insert into users(email, role, user_id, username, password)
values ('dame_un_beso@mail.ru', 'ADMIN', 1, 'admin', '$2a$12$kjIiKlk/ZFPEVHVRV970we5v2Sh4VIW54kVyZJZxXh6wCwLwx7QG6')
on conflict do nothing;
insert into patients(id, user_id, fullname, doctorId)
values (0, (select id from users where email = 'dame_un_beso@mail.ru'), 'ADMIN', 1)
on conflict do nothing;
