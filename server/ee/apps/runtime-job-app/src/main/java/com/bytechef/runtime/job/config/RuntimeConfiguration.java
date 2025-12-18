/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.config;

import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.runtime.job.platform.scheduler.NoOpTriggerScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableConfigurationProperties(ApplicationProperties.class)
@Configuration
public class RuntimeConfiguration {

    @Bean
    Evaluator evaluator() {
        return SpelEvaluator.create();
    }

    @Bean
    InMemoryContextRepository contextRepository() {
        return new InMemoryContextRepository();
    }

    @Bean
    InMemoryCounterRepository counterRepository() {
        return new InMemoryCounterRepository();
    }

    @Bean
    InMemoryJobRepository jobRepository(ObjectMapper objectMapper) {
        return new InMemoryJobRepository(taskExecutionRepository(), objectMapper);
    }

    @Bean
    InMemoryTaskExecutionRepository taskExecutionRepository() {
        return new InMemoryTaskExecutionRepository();
    }

    @Bean
    TriggerScheduler triggerScheduler() {
        return new NoOpTriggerScheduler();
    }
}
