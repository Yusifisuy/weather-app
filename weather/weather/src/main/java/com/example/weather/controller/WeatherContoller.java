package com.example.weather.controller;

import com.example.weather.service.WeatherService;
import com.example.weather.validation.CityNameConstraint;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/weather")
@Validated
public class WeatherContoller {


    private final WeatherService weatherService;


    public WeatherContoller(WeatherService weatherService) {
        this.weatherService = weatherService;
    }


    @GetMapping("/{city}")
    @RateLimiter(name = "basic")
    public ResponseEntity<?> getWeather(@PathVariable("city")@CityNameConstraint @NotBlank String city){
        return ResponseEntity.ok(weatherService.getWeatherByCityName(city));

    }
}
