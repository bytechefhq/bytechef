/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.api.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.discovery.redis.client.RedisDiscoveryClient;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
public class ApiGatewayApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.cloud.gateway.server.webmvc.routes[0].id"))
            .isEqualTo("user-graphql");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testRedisDiscoveryClientIsRegistered() {
        assertThat(applicationContext.getBeansOfType(RedisDiscoveryClient.class)).hasSize(1);
    }
}
