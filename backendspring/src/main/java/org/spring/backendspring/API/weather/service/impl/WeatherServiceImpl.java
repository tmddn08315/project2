package org.spring.backendspring.API.weather.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.spring.backendspring.API.weather.dto.WeatherApiDto;
import org.spring.backendspring.API.weather.entity.WeatherEntity;
import org.spring.backendspring.API.weather.repository.WeatherRepository;
import org.spring.backendspring.API.weather.service.WeatherService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class WeatherServiceImpl implements WeatherService {
    private final WeatherRepository weatherRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void insertWeather(String responseBody) {
        try {
            // 1. JSON → Java 객체 변환
            WeatherApiDto weatherApiDto = objectMapper.readValue(responseBody, WeatherApiDto.class);

            if (weatherApiDto.getName() == null || weatherApiDto.getCoord() == null || weatherApiDto.getSys() == null || weatherApiDto.getMain() == null) {
                log.warn("API 응답 데이터 필수 필드(name, coord, sys, main) 누락");
                return;
            }

            // DTO에서 데이터 추출 및 변환
            String name = weatherApiDto.getName();
            Double lat = weatherApiDto.getCoord().getLat();
            Double lon = weatherApiDto.getCoord().getLon();
            String country = weatherApiDto.getSys().getCountry();
            
            // icon은 weather 리스트의 첫 번째 항목에서 가져오되, 리스트가 비어있지 않은지 확인
            String icon = (weatherApiDto.getWeather() != null && !weatherApiDto.getWeather().isEmpty()) 
                          ? weatherApiDto.getWeather().get(0).getIcon() 
                          : null;
            
            // String 타입의 온도 필드를 Double로 변환 (Null 체크 및 파싱 오류 방지)
            double tempMin = Double.valueOf(weatherApiDto.getMain().getTemp_min());
            double tempMax = Double.valueOf(weatherApiDto.getMain().getTemp_max());


            // 2. 중복 체크: name, lat, lon, country를 기준으로 정확히 조회
            Optional<WeatherEntity> optionalEntity = weatherRepository.findByNameAndLatAndLonAndCountry(
                    name, lat, lon, country);
            
            WeatherEntity targetEntity;

            if (optionalEntity.isPresent()) {
                // 3. 이미 존재하는 경우: 최신 정보로 업데이트
                targetEntity = optionalEntity.get();
                targetEntity.setTemp_min(tempMin);
                targetEntity.setTemp_max(tempMax);
                targetEntity.setIcon(icon);

                log.info("기존 날씨 정보 업데이트 완료: ID={}", targetEntity.getId());
            } else {
                // 4. 새로운 경우: 새로운 엔티티 생성
                targetEntity = WeatherEntity.builder()
                        .name(name)
                        .lat(lat)
                        .lon(lon)
                        .country(country)
                        .icon(icon)
                        .temp_min(tempMin)
                        .temp_max(tempMax)
                        .build();

                log.info("새로운 날씨 정보 생성: {}", name);
            }

            // 5. 저장/업데이트 실행
            weatherRepository.save(targetEntity);
            log.info("DB 저장/업데이트 성공: {}", targetEntity);


        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("JSON 파싱 오류 발생 (API 응답 구조 확인 필요)", e);
        } catch (NumberFormatException e) {
            log.error("온도 필드(temp_min/max) Double 변환 오류 발생 (DTO 필드 타입 확인 필요)", e);
        } catch (Exception e) {
            log.error("날씨 정보 처리 중 예외 발생", e);
        }
    }
}