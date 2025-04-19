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
import org.springframework.cache.CacheManager;
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
    InMemoryContextRepository contextRepository(CacheManager cacheManager) {
        return new InMemoryContextRepository(cacheManager);
    }

    @Bean
    InMemoryCounterRepository counterRepository(CacheManager cacheManager) {
        return new InMemoryCounterRepository(cacheManager);
    }

    @Bean
    InMemoryJobRepository jobRepository(CacheManager cacheManager, ObjectMapper objectMapper) {
        return new InMemoryJobRepository(cacheManager, taskExecutionRepository(cacheManager), objectMapper);
    }

    @Bean
    InMemoryTaskExecutionRepository taskExecutionRepository(CacheManager cacheManager) {
        return new InMemoryTaskExecutionRepository(cacheManager);
    }
}
