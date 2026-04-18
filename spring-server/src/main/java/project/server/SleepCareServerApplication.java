package project.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SleepCareServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SleepCareServerApplication.class, args);
    }

}
