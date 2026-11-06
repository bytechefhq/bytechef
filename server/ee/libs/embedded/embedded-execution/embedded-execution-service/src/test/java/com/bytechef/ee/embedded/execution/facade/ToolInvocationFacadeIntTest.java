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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
    private ExternalInvocationLogService externalInvocationLogService;

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

        ExternalInvocationLog matchingExternalInvocationLog = createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR,
            connectedUserId, inRange);

        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR,
            otherConnectedUserId, inRange);
        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.PRODUCTION, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR, connectedUserId, inRange);
        createExternalInvocationLog(
            PlatformType.AUTOMATION, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR, connectedUserId, inRange);
        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ExternalInvocationSurface.EMBEDDED_API_TOOL,
            ExternalInvocationOutcome.ERROR, connectedUserId, inRange);
        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.SUCCESS, connectedUserId, inRange);
        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR,
            connectedUserId, Instant.parse("2026-07-18T12:00:00Z"));

        authenticate(EXTERNAL_USER_ID);

        Page<ExternalInvocationLog> externalInvocationLogPage = toolInvocationFacade.getToolInvocations(
            EXTERNAL_USER_ID, Environment.STAGING, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.ERROR,
            START_DATE, END_DATE, 0);

        assertThat(externalInvocationLogPage.getContent())
            .extracting(ExternalInvocationLog::getId)
            .containsExactly(matchingExternalInvocationLog.getId());
    }

    @Test
    void testGetToolInvocationsDeniesACallerAskingForAnotherConnectedUser() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser(
            EXTERNAL_USER_ID, Environment.PRODUCTION);

        createExternalInvocationLog(
            PlatformType.EMBEDDED, Environment.PRODUCTION, ExternalInvocationSurface.MCP_EMBEDDED,
            ExternalInvocationOutcome.SUCCESS, connectedUserId, Instant.now());

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

    private ExternalInvocationLog createExternalInvocationLog(
        PlatformType type, Environment environment, ExternalInvocationSurface surface,
        ExternalInvocationOutcome outcome,
        long connectedUserId, Instant createdDate) {

        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setConnectedUserId(connectedUserId);
        externalInvocationLog.setCreatedDate(createdDate);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setEnvironment(environment.ordinal());
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setOutcome(outcome);
        externalInvocationLog.setSurface(surface);
        externalInvocationLog.setToolName("slack_send");
        externalInvocationLog.setType(type);

        return externalInvocationLogService.create(externalInvocationLog);
    }
}
