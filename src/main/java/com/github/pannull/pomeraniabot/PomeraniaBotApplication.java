package com.github.pannull.pomeraniabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PomeraniaBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(PomeraniaBotApplication.class, args);
    }

}
