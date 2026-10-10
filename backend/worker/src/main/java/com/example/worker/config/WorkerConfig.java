package com.example.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WorkerConfig {

    @Bean
    public ExecutorService taskExecutorService() {
        return Executors.newCachedThreadPool();
    }
}
