package com.boundary.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoundaryApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoundaryApplication.class, args);
    }
}
