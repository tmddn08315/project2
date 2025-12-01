package org.spring.backendspring.API.weather.controller;

import lombok.RequiredArgsConstructor;

import org.spring.backendspring.API.weather.service.WeatherService;
import org.spring.backendspring.config.security.util.OpenApiUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class WeatherController {
    private final WeatherService weatherService;
    @Value("${open.openWeatherMap.serviceKey}")
    private String key;

    @GetMapping("/search/{q}")
    public ResponseEntity<?> search(@PathVariable("q") String q) {
        String apiURL = "http://api.openweathermap.org/data/2.5/weather?q=" + q + "&appid=" + key;
        
        // Header
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-type", "application/json");
        
        String responseBody = null;
        try {
            // API 호출 및 응답 받기
            responseBody = OpenApiUtil.get(apiURL, requestHeaders);
            
            if (responseBody == null || responseBody.contains("\"cod\":\"404\"")) {
                // API에서 도시를 찾지 못했거나 응답이 비어있는 경우
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "도시 정보를 찾을 수 없습니다: " + q));
            }

            System.out.println(responseBody + " responseBody");
            
            // JSON → Java 객체 변환 → DB 저장
            weatherService.insertWeather(responseBody);
            
            Map<String, String> weather = new HashMap<>();
            weather.put("weather", responseBody);
            return ResponseEntity.status(HttpStatus.OK).body(weather);

        } catch (Exception e) {
            System.err.println("날씨 API 호출 또는 처리 중 오류 발생: " + e.getMessage());
            // OpenApiUtil.get()에서 발생할 수 있는 일반적인 I/O 오류 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "날씨 정보 처리 중 서버 오류가 발생했습니다."));
        }
    }
}