package org.spring.backendspring.API.weather.dto;

import lombok.Data;

@Data
public class Main {
    private Double sea_level;
    private Double grnd_level;
    private Double feels_like;
    private Integer humidity; // 보통 Integer
    private Integer pressure; // 보통 Integer
    private Double temp;
    private Double temp_max; //최고 온도 <- Double로 수정
    private Double temp_min; //최저 온도 <- Double로 수정
}