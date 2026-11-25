package com.flowsuite.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FlowSuite — multi-tenant SaaS workspace platform API.
 *
 * <p>Bootstraps Spring Boot with JPA auditing (createdAt/updatedAt population),
 * scheduling (the monthly usage-reset job in Phase 3), and
 * {@code @ConfigurationProperties} scanning for the typed config beans.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@ConfigurationPropertiesScan
public class FlowSuiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowSuiteApplication.class, args);
    }
}
