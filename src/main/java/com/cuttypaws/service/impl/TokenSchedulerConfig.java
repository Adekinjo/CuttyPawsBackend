package com.cuttypaws.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TokenSchedulerConfig {

    @Bean(name = "tokenTaskScheduler")
    @Primary
    public TaskScheduler tokenTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("TokenScheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
