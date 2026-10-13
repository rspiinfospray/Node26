package org.infospray.peonsimulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class PeonSimulatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeonSimulatorApplication.class, args);
    }
}
