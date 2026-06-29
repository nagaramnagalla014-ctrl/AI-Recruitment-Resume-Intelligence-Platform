package com.recruitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Recruitment & Resume Intelligence Platform
 *
 * Entry point for the Spring Boot application. This platform provides
 * AI-assisted resume screening, candidate ranking, and interview question
 * generation using OpenAI GPT-4o integration with fallback rule-based logic.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class RecruitmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitmentApplication.class, args);
    }
}
