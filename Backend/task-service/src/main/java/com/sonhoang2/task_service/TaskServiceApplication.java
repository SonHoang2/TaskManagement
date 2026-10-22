package com.sonhoang2.task_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.sonhoang2.task_service.feign")
@SpringBootApplication(scanBasePackages = {"com.sonhoang2.task_service", "com.sonhoang2.common"})
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }

}
