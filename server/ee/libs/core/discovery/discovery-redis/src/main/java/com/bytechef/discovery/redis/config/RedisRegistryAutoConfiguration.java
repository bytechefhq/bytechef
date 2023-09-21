
/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    RedisServiceRegistry redisServiceRegistry(RedisTemplate<String, RedisRegistration> redisTemplate) {
        return new RedisServiceRegistry(redisTemplate);
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
