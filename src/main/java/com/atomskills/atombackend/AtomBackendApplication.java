package com.atomskills.atombackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtomBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtomBackendApplication.class, args);
    }

}
