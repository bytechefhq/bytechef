/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import io.modelcontextprotocol.common.McpTransportContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

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

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTransportContextCarriesConnectedUser() {
        User user = new User("ext-user-1", "", List.of());

        setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createMcpTransportContext(
            "server-secret", "PRODUCTION");

        assertThat(mcpTransportContext.get("externalUserId")).isEqualTo("ext-user-1");
        assertThat(mcpTransportContext.get("environment")).isEqualTo("PRODUCTION");
        assertThat(mcpTransportContext.get("secretKey")).isEqualTo("server-secret");
    }

    @Test
    void testTransportContextOmitsExternalUserForAnonymousCaller() {
        setAuthentication(McpAnonymousAuthenticationToken.ofEmbeddedMcpServer(1050L));

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createMcpTransportContext(
            "server-secret", null);

        assertThat(mcpTransportContext.get("externalUserId")).isNull();
        assertThat(mcpTransportContext.get("environment")).isNull();
        assertThat(mcpTransportContext.get("secretKey")).isEqualTo("server-secret");
    }

    private static void setAuthentication(org.springframework.security.core.Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}
