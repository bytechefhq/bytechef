/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkerApplication.class)
public class WorkerApplicationIntTest {

    private static final String AGENTIC_AI_COMPONENT_HANDLER_BEAN_NAME = "agenticAi_v1_ComponentHandler";

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testContextLoads() {
        assertThat(applicationContext.containsBean(AGENTIC_AI_COMPONENT_HANDLER_BEAN_NAME)).isFalse();
    }

    @Nested
    @ActiveProfiles({
        "testint", "agentic"
    })
    class AgenticProfile {

        @Autowired
        private ApplicationContext agenticApplicationContext;

        @Test
        void testAgenticAiComponentRegistered() {
            assertThat(agenticApplicationContext.containsBean(AGENTIC_AI_COMPONENT_HANDLER_BEAN_NAME)).isTrue();
        }
    }
}
