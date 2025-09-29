create table if not exists connections
(
    id               bigserial primary key,
    patient_id       bigint      not null,
    provider         varchar(32) not null,
    access_token     text,
    refresh_token    text,
    expires_at       timestamptz,
    scope            text,
    external_user_id varchar(255),
    member_id        varchar(255)
);



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



create unique index if not exists ux_connections_patient_provider
    on connections (patient_id, provider);



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

create table if not exists polar_sleep
(
    id                          bigserial primary key,
    patient_id                  bigint not null,
    date                        date   not null,

    sleep_start_time            timestamptz,
    sleep_end_time              timestamptz,
    device_id                   varchar(64),

    continuity                  real,
    continuity_class            integer,

    light_sleep                 integer,
    deep_sleep                  integer,
    rem_sleep                   integer,
    unrecognized_sleep_stage    integer,

    sleep_score                 integer,
    sleep_goal                  integer,
    sleep_rating                integer,

    total_interruption_duration integer,
    short_interruption_duration integer,
    long_interruption_duration  integer,

    sleep_cycles                integer,

    group_duration_score        integer,
    group_solidity_score        integer,
    group_regeneration_score    real,


    hypnogram_json              text,
    heart_rate_samples_json     text,

    created_at                  timestamptz default now(),
    updated_at                  timestamptz default now()
);

-- 1) polar_activities
create table if not exists polar_activities
(
    id                     bigserial primary key,
    patient_id             bigint,
    start_time             timestamptz,
    end_time               timestamptz,
    active_duration        varchar(32),
    inactive_duration      varchar(32),
    daily_activity         real,
    calories               integer,
    active_calories        integer,
    steps                  integer,
    inactivity_alert_count integer,
    distance_from_steps    real
);

-- 2) polar_cardio
create table if not exists polar_cardio
(
    id                 bigserial primary key,
    patient_id         bigint,
    date               timestamptz,
    cardio_load_status varchar(32),
    cardio_load_ratio  real,
    cardio_load        real,
    strain             real,
    tolerance          real
);

-- 3) polar_exercises
create table if not exists polar_exercises
(
    id                      bigserial primary key,
    patient_id              bigint,
    record_id               varchar(128),
    upload_time             timestamptz,
    device                  varchar(64),
    start_time              timestamptz,
    start_time_utc_offset   integer,
    duration                varchar(32),
    distance                real,
    sport                   varchar(64),
    has_route               boolean,
    detailed_sport_info     text,
    average_heart_rate      integer,
    max_heart_rate          integer,
    calories                integer,
    fat_percentage          integer,
    carbohydrate_percentage integer,
    protein_percentage      integer,
    device_id               varchar(64)
);

-- 4) polar_hr (heart rate samples)
create table if not exists polar_hr
(
    id            bigserial primary key,
    patient_id    bigint,
    polar_user_id bigint,
    date          date    not null,
    sample_time   time    not null,
    bpm           integer not null,
    created_at    timestamptz default now(),
    updated_at    timestamptz default now()
);
create unique index if not exists ux_polar_hr_patient_date_time
    on polar_hr (patient_id, date, sample_time);

-- 5) polar_nightrecharge
create table if not exists polar_nightrecharge
(
    id                         bigserial primary key,
    patient_id                 bigint,
    date                       timestamptz,
    heart_rate_avg             integer,
    beat_to_beat_avg           integer,
    heart_rate_variability_avg integer,
    breathing_rate_avg         real
);

-- 6) polar_spo (SpO2 tests/series)
create table if not exists polar_spo
(
    id                                 bigserial primary key,
    patient_id                         bigint,
    source_device_id                   varchar(64),
    test_time                          bigint,
    time_zone_offset                   integer,
    test_status                        varchar(32),
    blood_oxygen_percent               integer,
    spo2_class                         varchar(32),
    spo2_value_deviation_from_baseline varchar(32),
    spo2_quality_average_percent       real,
    average_heart_rate_bpm             integer,
    heart_rate_variability_ms          real,
    spo2_hrv_deviation_from_baseline   varchar(32),
    altitude_meters                    real,
    start_time                         timestamptz,
    end_time                           timestamptz
);

-- 7) polar_temperature
create table if not exists polar_temperature
(
    id               bigserial primary key,
    patient_id       bigint,
    source_device_id varchar(64),
    start_time       timestamptz,
    end_time         timestamptz,
    measurement_type varchar(32),
    sensor_location  varchar(64),
    samples          text
);

-- 8) polar_testecg
create table if not exists polar_testecg
(
    id                               bigserial primary key,
    source_device_id                 varchar(64),
    test_time                        bigint,
    patient_id                       bigint,
    time_zone_offset                 integer,
    average_heart_rate_bpm           integer,
    heart_rate_variability_ms        real,
    heart_rate_variability_level     varchar(32),
    rri_ms                           real,
    pulse_transit_time_systolic_ms   real,
    pulse_transit_time_diastolic_ms  real,
    pulse_transit_time_quality_index real,
    start_time                       timestamptz,
    end_time                         timestamptz
);

-- 9) polar_userinfo
create table if not exists polar_userinfo
(
    id                bigserial primary key,
    patient_id        bigint,
    polar_user_id     bigint not null,
    member_id         varchar(64),
    registration_date timestamptz,
    first_name        varchar(128),
    last_name         varchar(128),
    birthdate         date,
    gender            varchar(16),
    weight_kg         real,
    height_cm         real,
    created_at        timestamptz default now(),
    updated_at        timestamptz default now()
);
create table if not exists polar_temperature_sample
(
    id              bigserial primary key,
    patient_id      bigint,
    temperature_id  bigint,
    sample_time     timestamptz not null,
    value           real        not null,
    unit            varchar(16),
    created_at      timestamptz default now(),
    updated_at      timestamptz default now()
);
create table if not exists hypnogram
(
    id              bigserial primary key,
    patient_id      bigint,
    sleep_id        bigint,
    sleep_time      varchar(16),
    type_id         bigint,
    type_name       varchar(16),
    user_polar      varchar(128)
);
create table if not exists heart_rate_samples_sleep
(
    id              bigserial primary key,
    patient_id      bigint,
    sleep_id        bigint,
    sleep_time      varchar(16),
    value_hr         bigint,
    user_polar      varchar(128)
);
create table if not exists garmin_webhook_events
(
    id           bigserial primary key,
    patient_id   bigint,
    provider     varchar,
    event_type   varchar(256),
    user_id      varchar,
    summary_id   varchar(16),
    payload_json varchar(256),
    received_at  timestamp with time zone
);
create table if not exists garmin_activity (
                                               id bigserial primary key,
                                               patient_id bigint not null,
                                               activity_id varchar(255) unique,
                                               sport varchar(255),
                                               start_time timestamptz,
                                               end_time timestamptz,
                                               avg_hr int,
                                               max_hr int,
                                               calories int,
                                               distance_meters real,
                                               payload_json text
);

create table if not exists garmin_daily_summary (
                                                    id bigserial primary key,
                                                    patient_id bigint not null,
                                                    summary_id varchar(255) unique,
                                                    day varchar(20),          -- формат YYYY-MM-DD
                                                    steps int,
                                                    calories int,
                                                    stress int,
                                                    body_battery int,
                                                    payload_json text,
                                                    updated_at timestamptz
);

create table if not exists garmin_sleep (
                                            id bigserial primary key,
                                            patient_id bigint not null,
                                            sleep_id varchar(255) unique,
                                            start_time timestamptz,
                                            end_time timestamptz,
                                            score int,
                                            duration_sec int,
                                            payload_json text
);

insert into users(email, role, user_id, username, password)
values ('dame_un_beso@mail.ru', 'ADMIN', 1, 'admin', '$2a$12$kjIiKlk/ZFPEVHVRV970we5v2Sh4VIW54kVyZJZxXh6wCwLwx7QG6')
on conflict do nothing;
insert into patients(id, user_id, fullname, doctorId)
values (0, (select id from users where email = 'dame_un_beso@mail.ru'), 'ADMIN', 1)
on conflict do nothing;
