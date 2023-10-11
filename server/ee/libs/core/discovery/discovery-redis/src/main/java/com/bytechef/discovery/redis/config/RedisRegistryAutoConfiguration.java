
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.discovery.redis.config;

import com.bytechef.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.discovery.redis.client.RedisDiscoveryClient;
import com.bytechef.discovery.redis.client.RedisReactiveDiscoveryClient;
import com.bytechef.discovery.redis.metadata.RedisServiceMetadataRegistry;
import com.bytechef.discovery.redis.registry.RedisAutoServiceRegistration;
import com.bytechef.discovery.redis.registry.RedisRegistration;
import com.bytechef.discovery.redis.registry.RedisServiceRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.redis.enabled", matchIfMissing = true)
@AutoConfigureBefore({
    CommonsClientAutoConfiguration.class, ServiceRegistryAutoConfiguration.class
})
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisRegistryAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisRegistryAutoConfiguration.class);

    public RedisRegistryAutoConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Discovery service provider type enabled: redis");
        }
    }

    @Bean
    RedisAutoServiceRegistration redisAutoServiceRegistration(
        RedisServiceRegistry redisServiceRegistry, AutoServiceRegistrationProperties properties) {

        return new RedisAutoServiceRegistration(redisServiceRegistry, properties, redisRegistration());
    }

    @Bean
    RedisReactiveDiscoveryClient redisReactiveDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        return new RedisReactiveDiscoveryClient(redisTemplate);
    }

    @Bean
    RedisDiscoveryClient redisDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        return new RedisDiscoveryClient(redisTemplate);
    }

    @Bean
    ServiceMetadataRegistry serviceMetadataRegistrar() {
        return new RedisServiceMetadataRegistry(redisRegistration());
    }

    @Bean
    RedisRegistration redisRegistration() {
        return new RedisRegistration();
    }

    @Bean
    RedisServiceRegistry redisServiceRegistry(
        RedisTemplate<String, RedisRegistration> redisTemplate, TaskExecutor taskExecutor) {
        return new RedisServiceRegistry(redisTemplate, taskExecutor);
    }

    @Bean
    RedisTemplate<String, RedisRegistration> redisRegistrationRedisTemplate(
        ObjectMapper objectMapper, RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, RedisRegistration> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, RedisRegistration.class));

        return redisTemplate;
    }
}
