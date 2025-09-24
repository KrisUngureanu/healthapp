package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="whoop_recovery")
public class WhoopRecovery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int record_id;
    private int cycle_id; //смотрим на whoopcycle record_id
    private String sleep_id; //смотрим на whoopsleep record_id
    private int userid;
    private Long patient_id;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private String score_state;

    private boolean user_calibrating;
    private float recovery_score; //в процентах готовность организма к нагрузке
    private float resting_heart_rate; //Частота сердечных сокращений пользователя в состоянии покоя
    private float hrv_rmssd_milli; //Вариабельность сердечного ритма пользователя, измеренная с помощью среднеквадратичного отклонения последовательных разностей (RMSSD), в миллисекундах.
    private float spo2_percentage;//Процентное содержание кислорода в крови пользователя.
    private float skin_temp_celsius; //Температура кожи пользователя в градусах Цельсия

}
