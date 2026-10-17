/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.coordinator.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT1H")
public class SchedulerLockConfiguration {

    static final String LOCK_ENVIRONMENT = "bytechef-coordinator";

    @Bean
    LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory, LOCK_ENVIRONMENT);
    }
}
