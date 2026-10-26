/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import(RedisContainerConfiguration.class)
@SpringBootTest(classes = WebhookApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebhookApplicationIntTest {

    @Autowired
    private Environment environment;

    @Test
    public void testContextLoads() {
    }

    @Test
    public void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix")).isEqualTo("webhook-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }
}
