package iuh.fit.airsky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirsKyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirsKyApplication.class, args);
    }

}
