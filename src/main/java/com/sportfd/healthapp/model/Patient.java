package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity @Table(name="patients")
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String fullname;
    @Column(name = "doctorid")
    private Long doctorId;
    private String status;
    private String device;
    private LocalDate birthDate;
    private String gender;
    private String jsonData;
    private double HeightCm;
    private double WeightKg;
    private short RhrBpm;
    private short HrvRmssdMs;
    private String BodyMeasurementJson;
    public Patient() {}


    public void setSex(String gender) {
        this.gender = gender;
    }

    public void setProfileJson(String string) {
        this.jsonData = string;
    }

}
