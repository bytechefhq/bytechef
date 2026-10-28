/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.discovery.redis.client.RedisDiscoveryClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 */
@Testcontainers
class RedisServiceRegistryIntTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private LettuceConnectionFactory lettuceConnectionFactory;
    private RedisDiscoveryClient redisDiscoveryClient;
    private RedisServiceRegistry redisServiceRegistry;
    private RedisTemplate<String, RedisRegistration> redisTemplate;

    @BeforeEach
    void beforeEach() {
        lettuceConnectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));

        lettuceConnectionFactory.afterPropertiesSet();
        lettuceConnectionFactory.start();

        redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(
            new JacksonJsonRedisSerializer<>(
                JsonMapper.builder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build(),
                RedisRegistration.class));
        redisTemplate.afterPropertiesSet();

        redisTemplate.delete("discovery/worker-app");

        Set<String> heartbeatKeys = redisTemplate.keys("discovery-heartbeat/*");

        if (heartbeatKeys != null) {
            redisTemplate.delete(heartbeatKeys);
        }

        redisDiscoveryClient = new RedisDiscoveryClient(redisTemplate);
        redisServiceRegistry = new RedisServiceRegistry(redisTemplate, runnable -> {});
    }

    @AfterEach
    void afterEach() {
        lettuceConnectionFactory.destroy();
    }

    @Test
    void testRegisteringAgainWithChangedMetadataReplacesTheInstance() {
        RedisRegistration redisRegistration = redisRegistration("worker-app:1");

        redisServiceRegistry.register(redisRegistration);

        redisRegistration.setMetadata(Map.of("grpc-port", "9090"));

        redisServiceRegistry.register(redisRegistration);

        List<ServiceInstance> serviceInstances = redisDiscoveryClient.getInstances("worker-app");

        assertThat(serviceInstances).singleElement()
            .satisfies(serviceInstance -> assertThat(serviceInstance.getMetadata()).containsEntry("grpc-port", "9090"));
    }

    @Test
    void testRegisteringOneInstanceKeepsTheOthers() {
        RedisRegistration firstRedisRegistration = redisRegistration("worker-app:1");
        RedisRegistration secondRedisRegistration = redisRegistration("worker-app:2");

        redisServiceRegistry.register(firstRedisRegistration);
        redisServiceRegistry.register(secondRedisRegistration);

        firstRedisRegistration.setMetadata(Map.of("grpc-port", "9090"));

        redisServiceRegistry.register(firstRedisRegistration);

        assertThat(redisDiscoveryClient.getInstances("worker-app")).extracting(ServiceInstance::getInstanceId)
            .containsExactlyInAnyOrder("worker-app:1", "worker-app:2");
    }

    @Test
    void testDeregisterRemovesTheInstance() {
        RedisRegistration redisRegistration = redisRegistration("worker-app:1");

        redisServiceRegistry.register(redisRegistration);

        redisRegistration.setMetadata(Map.of("grpc-port", "9090"));

        redisServiceRegistry.deregister(redisRegistration);

        assertThat(redisDiscoveryClient.getInstances("worker-app")).isEmpty();
    }

    @Test
    void testRegisteringPrunesAnInstanceThatStoppedHeartbeating() {
        RedisRegistration crashedRedisRegistration = redisRegistration("worker-app:crashed");

        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        listOperations.leftPush("discovery/worker-app", crashedRedisRegistration);

        redisServiceRegistry.register(redisRegistration("worker-app:1"));

        assertThat(redisDiscoveryClient.getInstances("worker-app")).extracting(ServiceInstance::getInstanceId)
            .containsExactly("worker-app:1");
    }

    @Test
    void testRegisteringKeepsAnotherInstanceThatIsStillHeartbeating() {
        RedisServiceRegistry otherRedisServiceRegistry = new RedisServiceRegistry(redisTemplate, runnable -> {});

        otherRedisServiceRegistry.register(redisRegistration("worker-app:2"));

        redisServiceRegistry.register(redisRegistration("worker-app:1"));
        redisServiceRegistry.register(redisRegistration("worker-app:1"));

        assertThat(redisDiscoveryClient.getInstances("worker-app")).extracting(ServiceInstance::getInstanceId)
            .containsExactlyInAnyOrder("worker-app:1", "worker-app:2");
    }

    private static RedisRegistration redisRegistration(String instanceId) {
        RedisRegistration redisRegistration = new RedisRegistration();

        ReflectionTestUtils.setField(redisRegistration, "host", "localhost");
        ReflectionTestUtils.setField(redisRegistration, "instanceId", instanceId);
        ReflectionTestUtils.setField(redisRegistration, "port", 8080);
        ReflectionTestUtils.setField(redisRegistration, "serviceId", "worker-app");

        return redisRegistration;
    }
}
