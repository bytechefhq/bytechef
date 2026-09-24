/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerConfigurationTest {

    @Test
    void testEmbeddedMcpToolFacadeRunsWorkflowToolsOnTheSynchronousWorkerExecutor() {
        Method embeddedMcpToolFacadeMethod = Arrays.stream(EmbeddedMcpServerConfiguration.class.getDeclaredMethods())
            .filter(method -> method.getReturnType() == EmbeddedMcpToolFacade.class)
            .findFirst()
            .orElseThrow();

        List<Parameter> taskExecutorParameters = Arrays.stream(embeddedMcpToolFacadeMethod.getParameters())
            .filter(parameter -> TaskExecutor.class.isAssignableFrom(parameter.getType()))
            .toList();

        assertThat(taskExecutorParameters).hasSize(1);

        Qualifier qualifier = taskExecutorParameters.getFirst()
            .getAnnotation(Qualifier.class);

        assertThat(qualifier).isNotNull();
        assertThat(qualifier.value()).isEqualTo("syncWorkerExecutor");
    }
}
