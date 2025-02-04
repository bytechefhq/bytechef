/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.config;

import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.config.ApplicationProperties;
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
}
