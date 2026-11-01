/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogRetentionJob;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@Import({
    PostgreSQLContainerConfiguration.class
})
public class ExecutionApplicationIntTest {

    @Autowired
    private ObjectProvider<ToolInvocationLogRetentionJob> toolInvocationLogRetentionJobProvider;

    @Test
    void testContextLoads() {
    }

    @Test
    void testToolInvocationLogRetentionJobPurgesTheExecutionDatabase() {
        ToolInvocationLogRetentionJob toolInvocationLogRetentionJob =
            toolInvocationLogRetentionJobProvider.getIfAvailable();

        assertThat(toolInvocationLogRetentionJob).isNotNull();

        assertThatCode(toolInvocationLogRetentionJob::purgeExpiredLogs).doesNotThrowAnyException();
    }
}
