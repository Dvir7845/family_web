package com.familyhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FamilyHomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyHomeApplication.class, args);
    }
}
