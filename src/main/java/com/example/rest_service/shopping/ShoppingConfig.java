package com.example.rest_service.shopping;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ShoppingConfig {

    @Bean
    public Clock shoppingClock() {
        return Clock.systemUTC();
    }
}