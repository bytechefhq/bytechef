/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.configuration.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowCacheEvictAspectIntTest.TestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class WorkflowCacheEvictAspectIntTest {

    @Configuration
    @ComponentScan(
        basePackages = {
            "com.bytechef.platform.configuration.cache"
        })
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class
    })
    static class TestConfiguration {
    }

    @Autowired
    private WorkflowCacheEvictTestService workflowCacheEvictTestService;

    @MockitoBean
    private WorkflowCacheManager workflowCacheManager;

    @BeforeEach
    void setUp() {
        workflowCacheEvictTestService.reset();
    }

    @AfterEach
    void tearDown() {
        workflowCacheEvictTestService.reset();
    }

    @Test
    void testEvictSingleCacheExtractsAnnotatedParameters() {
        workflowCacheEvictTestService.setBehavior(() -> "success");

        Object result = workflowCacheEvictTestService.evictSingleCache("test-workflow-id", "node-name", 123L);

        assertThat(result).isEqualTo("success");
        assertThat(workflowCacheEvictTestService.getCallCount()).isEqualTo(1);

        verify(workflowCacheManager).clearCacheForWorkflow("test-workflow-id", "testCache", 123L);
        verifyNoMoreInteractions(workflowCacheManager);
    }

    @Test
    void testEvictMultipleCaches() {
        workflowCacheEvictTestService.setBehavior(() -> "multiple_success");

        Object result = workflowCacheEvictTestService.evictMultipleCaches("workflow-123", 456L);

        assertThat(result).isEqualTo("multiple_success");
        assertThat(workflowCacheEvictTestService.getCallCount()).isEqualTo(1);

        verify(workflowCacheManager).clearCacheForWorkflow("workflow-123", "cache1", 456L);
        verify(workflowCacheManager).clearCacheForWorkflow("workflow-123", "cache2", 456L);
        verify(workflowCacheManager).clearCacheForWorkflow("workflow-123", "cache3", 456L);
        verifyNoMoreInteractions(workflowCacheManager);
    }

    @Test
    void testEvictWithParametersInDifferentOrder() {
        workflowCacheEvictTestService.setBehavior(() -> "reversed_success");

        Object result = workflowCacheEvictTestService.evictWithReversedParameters(789L, "other-param", "workflow-xyz");

        assertThat(result).isEqualTo("reversed_success");
        assertThat(workflowCacheEvictTestService.getCallCount()).isEqualTo(1);

        verify(workflowCacheManager).clearCacheForWorkflow("workflow-xyz", "reversedCache", 789L);
        verifyNoMoreInteractions(workflowCacheManager);
    }

    @Test
    void testCacheEvictionOnlyHappensAfterMethodReturns() {
        workflowCacheEvictTestService.setBehavior(() -> "after_return");

        Object result = workflowCacheEvictTestService.evictSingleCache("workflow-after", "node", 100L);

        assertThat(result).isEqualTo("after_return");

        verify(workflowCacheManager).clearCacheForWorkflow("workflow-after", "testCache", 100L);
    }
}
