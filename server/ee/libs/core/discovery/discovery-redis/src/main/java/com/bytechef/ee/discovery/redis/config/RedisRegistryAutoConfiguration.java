/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.config;

import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.ee.discovery.redis.client.RedisDiscoveryClient;
import com.bytechef.ee.discovery.redis.client.RedisReactiveDiscoveryClient;
import com.bytechef.ee.discovery.redis.metadata.RedisServiceMetadataRegistry;
import com.bytechef.ee.discovery.redis.registry.RedisAutoServiceRegistration;
import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import com.bytechef.ee.discovery.redis.registry.RedisServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.redis.enabled", matchIfMissing = true)
@AutoConfigureBefore({
    CommonsClientAutoConfiguration.class, ServiceRegistryAutoConfiguration.class
})
@AutoConfigureAfter(DataRedisAutoConfiguration.class)
public class RedisRegistryAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisRegistryAutoConfiguration.class);

    public RedisRegistryAutoConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Discovery service provider type enabled: redis");
        }
    }

    @Bean
    RedisAutoServiceRegistration redisAutoServiceRegistration(
        ApplicationContext applicationContext, RedisServiceRegistry redisServiceRegistry,
        AutoServiceRegistrationProperties properties) {

        return new RedisAutoServiceRegistration(
            applicationContext, redisServiceRegistry, properties, redisRegistration());
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
        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(objectMapper, RedisRegistration.class));

        return redisTemplate;
    }
}
