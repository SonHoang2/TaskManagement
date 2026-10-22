package com.sonhoang2.sprint_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.sonhoang2.sprint_service.feign")
@SpringBootApplication
public class SprintServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SprintServiceApplication.class, args);
	}

}
