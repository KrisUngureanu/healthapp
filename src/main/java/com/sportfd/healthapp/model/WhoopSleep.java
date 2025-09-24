package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="whoop_sleep")
public class WhoopSleep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int userid;
    private Long patient_id;
    private Long cycle_id; //смотрим на whoopcycle record_id

    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private OffsetDateTime start;
    private OffsetDateTime enddate;

    private String timezone_offset;
    private String score_state;

    private boolean nap;

    private int total_in_bed_time_milli; // Общее время, которое пользователь провёл в постели, в миллисекундах
    private int total_awake_time_milli; //Общее время, в течение которого пользователь бодрствовал, в миллисекундах
    private int total_no_data_time_milli; //Общее время, в течение которого WHOOP не получал данные от пользователя во время сна, в миллисекундах
    private int total_light_sleep_time_milli; //Общее время, проведенное пользователем в состоянии легкого сна
    private int total_slow_wave_sleep_time_milli; //Общее время, проведенное пользователем в медленном сне (SWS), в миллисекундах
    private int total_rem_sleep_time_milli; //Общее время, проведенное пользователем в режиме быстрого сна (REM), в миллисекундах
    private int sleep_cycle_count; //Количество циклов ожидания во время сна пользователя
    private int disturbance_count; //Количество раз, когда пользователя беспокоили во время сна

    private int baseline_milli; //Количество сна, необходимое пользователю, исходя из исторических тенденций
    private int need_from_sleep_debt_milli; //Разница между количеством сна, необходимым организму пользователя, и количеством сна, которое он фактически получил
    private int need_from_recent_strain_milli; //Потребность в дополнительном сне увеличивается в зависимости от нагрузки пользователя
    private int need_from_recent_nap_milli; //Снижение потребности во сне, рассчитанное на основе недавней дневной активности пользователя (отрицательное значение или ноль)

    private float respiratory_rate; //Частота дыхания пользователя во время сна.
    private float sleep_performance_percentage; //Процент (0-100%) времени, в течение которого пользователь спит, от необходимого ему количества сна.
    private float sleep_consistency_percentage;  //Процент (0-100%) от того, насколько это время сна и бодрствования совпадает с предыдущим днем
    private float sleep_efficiency_percentage;  //Процентное соотношение (0-100%) времени, которое вы проводите в постели, к тому времени, когда вы на самом деле спите.
}
