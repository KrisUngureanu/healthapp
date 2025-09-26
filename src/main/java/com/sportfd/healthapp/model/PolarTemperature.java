package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="polar_temperature")
public class PolarTemperature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String source_device_id;
    @Column(name = "start_time")
    private OffsetDateTime startTime;
    @Column(name = "end_time")
    private OffsetDateTime endTime;
    private String measurement_type;
    private String sensor_location;

    @Column(columnDefinition = "text")
    private String samples;

}
