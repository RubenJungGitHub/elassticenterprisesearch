package contain.opensource.enterprisesearch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // Minstens 2 threads zodat Alfresco en SharePoint tegelijk kunnen draaien
        scheduler.setThreadNamePrefix("EnterpriseSearch-Scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}