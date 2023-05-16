package com.example.weather.service;

import com.example.weather.constants.Constants;
import com.example.weather.dto.WeatherDto;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.model.WeatherEntity;
import com.example.weather.repository.WeatherRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.hibernate.annotations.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"weathers"})
public class WeatherService {

    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(WeatherRepository weatherRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }




    private String getWeatherApiUrl(String city){
        return Constants.API_URL + Constants.ACCESS_KEY_PARAM + Constants.API_URL + Constants.QUERY_KEY_PARAM + city;
    }

    @Cacheable(key = "#city")
    public WeatherDto getWeatherByCityName(String city){
        Optional<WeatherEntity> weather = weatherRepository.findFirstByRequestCityNameOrderByUpdatedTimeDesc(city);

        return weather.map(weatherEntity -> {
            if (weatherEntity.getUpdatedTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
                return WeatherDto.convert(getWeatherFromWeatherStack(city));
            }
            return WeatherDto.convert(weatherEntity);
        }).orElseGet(() -> WeatherDto.convert(getWeatherFromWeatherStack(city)));
    }


    private WeatherEntity getWeatherFromWeatherStack(String cityName) {
        ResponseEntity<String> response = restTemplate.getForEntity(getWeatherApiUrl(cityName), String.class);
        try {
            WeatherResponse weatherResponse = objectMapper.readValue(response.getBody(),WeatherResponse.class);
            return saveWeatherEntity(cityName,weatherResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }


    @CacheEvict(allEntries = true)
    @PostConstruct
    @Scheduled(fixedRateString = "10000")
    public void clearCache(){

    }

    private WeatherEntity saveWeatherEntity(String city,WeatherResponse weatherResponse){

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


        WeatherEntity weatherEntity = WeatherEntity.builder()
                .requestCityName(city).cityName(weatherResponse.location().name())
                .country(weatherResponse.location().country())
                .temperature(weatherResponse.current().temperature())
                .updatedTime(LocalDateTime.now())
                .responseLocalTime(LocalDateTime.parse(weatherResponse.location().localtime(),dateTimeFormatter)).build();

        return weatherRepository.save(weatherEntity);
    }
}
