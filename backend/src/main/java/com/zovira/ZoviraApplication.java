package com.zovira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ZoviraApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZoviraApplication.class, args);
    }
}
