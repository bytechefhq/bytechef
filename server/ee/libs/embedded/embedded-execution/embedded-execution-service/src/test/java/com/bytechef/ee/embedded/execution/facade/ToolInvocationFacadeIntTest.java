/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestSupport;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedExecutionIntTestConfiguration.class)
class ToolInvocationFacadeIntTest {

    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final String EXTERNAL_USER_ID = "external-user-1";
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    @Autowired
    private EmbeddedExecutionIntTestSupport embeddedExecutionIntTestSupport;

    @Autowired
    private ToolInvocationFacade toolInvocationFacade;

    @Autowired
    private ToolInvocationLogService toolInvocationLogService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        embeddedExecutionIntTestSupport.deleteAll();
    }

    @Test
    void testGetToolInvocationsReadsTheEmbeddedLogOfTheCallingConnectedUserInTheEnvironment() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser(
            EXTERNAL_USER_ID, Environment.STAGING);
        long otherConnectedUserId = embeddedExecutionIntTestSupport.createConnectedUser(
            "external-user-2", Environment.STAGING);

        Instant inRange = Instant.parse("2026-07-18T10:30:00Z");

        ToolInvocationLog matchingToolInvocationLog = createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionOutcome.ERROR,
            connectedUserId, inRange);

        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionOutcome.ERROR,
            otherConnectedUserId, inRange);
        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.PRODUCTION, ToolExecutionSurface.MCP_EMBEDDED,
            ToolExecutionOutcome.ERROR, connectedUserId, inRange);
        createToolInvocationLog(
            PlatformType.AUTOMATION, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED,
            ToolExecutionOutcome.ERROR, connectedUserId, inRange);
        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ToolExecutionSurface.EMBEDDED_API_TOOL,
            ToolExecutionOutcome.ERROR, connectedUserId, inRange);
        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED,
            ToolExecutionOutcome.SUCCESS, connectedUserId, inRange);
        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionOutcome.ERROR,
            connectedUserId, Instant.parse("2026-07-18T12:00:00Z"));

        authenticate(EXTERNAL_USER_ID);

        Page<ToolInvocationLog> toolInvocationLogPage = toolInvocationFacade.getToolInvocations(
            EXTERNAL_USER_ID, Environment.STAGING, ToolExecutionSurface.MCP_EMBEDDED, ToolExecutionOutcome.ERROR,
            START_DATE, END_DATE, 0);

        assertThat(toolInvocationLogPage.getContent())
            .extracting(ToolInvocationLog::getId)
            .containsExactly(matchingToolInvocationLog.getId());
    }

    @Test
    void testGetToolInvocationsDeniesACallerAskingForAnotherConnectedUser() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser(
            EXTERNAL_USER_ID, Environment.PRODUCTION);

        createToolInvocationLog(
            PlatformType.EMBEDDED, Environment.PRODUCTION, ToolExecutionSurface.MCP_EMBEDDED,
            ToolExecutionOutcome.SUCCESS, connectedUserId, Instant.now());

        authenticate("external-user-2");

        assertThatThrownBy(
            () -> toolInvocationFacade.getToolInvocations(
                EXTERNAL_USER_ID, Environment.PRODUCTION, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticate(String name) {
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(name, "n/a", List.of()));
    }

    private ToolInvocationLog createToolInvocationLog(
        PlatformType type, Environment environment, ToolExecutionSurface surface, ToolExecutionOutcome outcome,
        long connectedUserId, Instant createdDate) {

        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setConnectedUserId(connectedUserId);
        toolInvocationLog.setCreatedDate(createdDate);
        toolInvocationLog.setDurationMs(5);
        toolInvocationLog.setEnvironment(environment.ordinal());
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setOutcome(outcome);
        toolInvocationLog.setSurface(surface);
        toolInvocationLog.setToolName("slack_send");
        toolInvocationLog.setType(type);

        return toolInvocationLogService.create(toolInvocationLog);
    }
}
