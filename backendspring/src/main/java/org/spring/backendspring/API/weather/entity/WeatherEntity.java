package org.spring.backendspring.API.weather.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "weather29") // 테이블 이름이 'weather29'라고 가정합니다.
public class WeatherEntity {

    @Id
    @Column(name = "weather_id") // DB 컬럼명 명시: weather_id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name") // DB 컬럼명 명시: name
    private String name;
    
    @Column(name = "lat") // DB 컬럼명 명시: lat
    private double lat;
    
    @Column(name = "lon") // DB 컬럼명 명시: lon
    private double lon;
    
    @Column(name = "country") // DB 컬럼명 명시: country
    private String country;
    
    @Column(name = "temp_max") // DB 컬럼명 명시: temp_max
    private double temp_max;
    
    @Column(name = "temp_min") // DB 컬럼명 명시: temp_min
    private double temp_min;
    
    @Column(name = "icon") // DB 컬럼명 명시: icon
    private String icon;
}