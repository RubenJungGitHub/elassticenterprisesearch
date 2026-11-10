package contain.opensource.enterprisesearch;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Activeert automatisch alle @Scheduled taken (zoals je AlfrescoSyncScheduler)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}