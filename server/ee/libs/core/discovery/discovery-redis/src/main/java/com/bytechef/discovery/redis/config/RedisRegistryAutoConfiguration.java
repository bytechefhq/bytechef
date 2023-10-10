
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
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
