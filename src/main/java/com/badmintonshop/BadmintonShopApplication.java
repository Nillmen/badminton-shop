package com.badmintonshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class BadmintonShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BadmintonShopApplication.class, args);
    }

}
