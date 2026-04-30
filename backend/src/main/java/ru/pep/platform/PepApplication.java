package ru.pep.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PepApplication {

    public static void main(String[] args) {
        SpringApplication.run(PepApplication.class, args);
    }
}
