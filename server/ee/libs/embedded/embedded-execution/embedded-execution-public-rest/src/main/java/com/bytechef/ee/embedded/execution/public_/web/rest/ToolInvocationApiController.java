/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationOutcomeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationSurfaceModel;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read-only tool-invocation history endpoint of the embedded API. Results are always scoped to the connected
 * user resolved from the authenticated principal and never carry tool input or output payloads.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.execution.public_.web.rest.ToolInvocationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ToolInvocationApiController implements ToolInvocationApi {

    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;
    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI2")
    public ToolInvocationApiController(
        ConnectedUserService connectedUserService, EnvironmentService environmentService,
        ToolInvocationLogService toolInvocationLogService) {

        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public ResponseEntity<Page> getToolInvocationsPage(
        String externalUserId, @Nullable EnvironmentModel xEnvironment, @Nullable ToolInvocationSurfaceModel surface,
        @Nullable ToolInvocationOutcomeModel outcome, @Nullable OffsetDateTime startDate,
        @Nullable OffsetDateTime endDate, Integer pageNumber) {

        SecurityUtils.checkCurrentUserLogin(externalUserId);

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Page<ToolInvocationLog> toolInvocationLogsPage = toolInvocationLogService.getToolInvocationLogs(
            surface == null ? null : ToolExecutionSurface.valueOf(surface.name()),
            outcome == null ? null : ToolExecutionOutcome.valueOf(outcome.name()), null, connectedUser.getId(), null,
            toInstant(startDate), toInstant(endDate), pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(toolInvocationLogsPage.map(this::toModel));
    }

    private ToolInvocationModel toModel(ToolInvocationLog toolInvocationLog) {
        return new ToolInvocationModel()
            .id(toolInvocationLog.getId())
            .surface(ToolInvocationSurfaceModel.valueOf(toolInvocationLog.getSurface()
                .name()))
            .kind(ToolInvocationKindModel.valueOf(toolInvocationLog.getKind()
                .name()))
            .toolName(toolInvocationLog.getToolName())
            .componentName(toolInvocationLog.getComponentName())
            .componentVersion(toolInvocationLog.getComponentVersion())
            .operationName(toolInvocationLog.getOperationName())
            .integrationInstanceId(toolInvocationLog.getIntegrationInstanceId())
            .jobId(toolInvocationLog.getJobId())
            .outcome(ToolInvocationOutcomeModel.valueOf(toolInvocationLog.getOutcome()
                .name()))
            .errorMessage(toolInvocationLog.getErrorMessage())
            .durationMs(toolInvocationLog.getDurationMs())
            .createdDate(toOffsetDateTime(toolInvocationLog.getCreatedDate()));
    }

    private @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
