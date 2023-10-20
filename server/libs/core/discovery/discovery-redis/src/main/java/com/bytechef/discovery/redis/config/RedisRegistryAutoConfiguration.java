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

import com.bytechef.discovery.redis.client.RedisDiscoveryClient;
import com.bytechef.discovery.redis.client.RedisReactiveDiscoveryClient;
import com.bytechef.discovery.redis.registry.RedisAutoServiceRegistration;
import com.bytechef.discovery.redis.registry.RedisRegistration;
import com.bytechef.discovery.redis.registry.RedisServiceRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.redis.enabled", matchIfMissing = true)
@AutoConfigureBefore({CommonsClientAutoConfiguration.class, ServiceRegistryAutoConfiguration.class})
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisRegistryAutoConfiguration {

    @Bean
    RedisAutoServiceRegistration redisAutoServiceRegistration(
            RedisServiceRegistry redisServiceRegistry, AutoServiceRegistrationProperties properties) {
        return new RedisAutoServiceRegistration(redisServiceRegistry, properties);
    }

    @Bean
    RedisReactiveDiscoveryClient redisReactiveDiscoveryClient(StringRedisTemplate redisTemplate) {
        return new RedisReactiveDiscoveryClient(redisTemplate);
    }

    @Bean
    RedisDiscoveryClient redisDiscoveryClient(StringRedisTemplate redisTemplate) {
        return new RedisDiscoveryClient(redisTemplate);
    }

    @Bean
    RedisRegistration redisRegistration() {
        return new RedisRegistration();
    }

    @Bean
    RedisServiceRegistry redisServiceRegistry(StringRedisTemplate redisTemplate) {
        return new RedisServiceRegistry(redisTemplate);
    }
}
