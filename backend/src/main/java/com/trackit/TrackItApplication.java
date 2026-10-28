package com.trackit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrackItApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackItApplication.class, args);
    }
}
