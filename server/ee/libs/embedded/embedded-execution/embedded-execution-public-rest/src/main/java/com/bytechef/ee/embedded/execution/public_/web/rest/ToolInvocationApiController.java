/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.execution.facade.ToolInvocationFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationOutcomeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationPageModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationSurfaceModel;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ToolInvocationApiController implements ToolInvocationApi {

    private final EnvironmentService environmentService;
    private final ToolInvocationFacade toolInvocationFacade;

    @SuppressFBWarnings("EI")
    public ToolInvocationApiController(
        EnvironmentService environmentService, ToolInvocationFacade toolInvocationFacade) {

        this.environmentService = environmentService;
        this.toolInvocationFacade = toolInvocationFacade;
    }

    @Override
    public ResponseEntity<ToolInvocationPageModel> getToolInvocationsPage(
        String externalUserId, @Nullable EnvironmentModel xEnvironment, @Nullable ToolInvocationSurfaceModel surface,
        @Nullable ToolInvocationOutcomeModel outcome, @Nullable OffsetDateTime startDate,
        @Nullable OffsetDateTime endDate, @Nullable Integer pageNumber) {

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        Page<ToolInvocationLog> toolInvocationLogPage = toolInvocationFacade.getToolInvocations(
            externalUserId, environment, surface == null ? null : ToolExecutionSurface.valueOf(surface.name()),
            outcome == null ? null : ToolExecutionOutcome.valueOf(outcome.name()), toInstant(startDate),
            toInstant(endDate), pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(toToolInvocationPageModel(toolInvocationLogPage));
    }

    static ToolInvocationKindModel toToolInvocationKindModel(ToolExecutionKind kind) {
        return ToolInvocationKindModel.valueOf(kind.name());
    }

    static ToolInvocationOutcomeModel toToolInvocationOutcomeModel(ToolExecutionOutcome outcome) {
        return ToolInvocationOutcomeModel.valueOf(outcome.name());
    }

    static ToolInvocationSurfaceModel toToolInvocationSurfaceModel(ToolExecutionSurface surface) {
        return ToolInvocationSurfaceModel.valueOf(surface.name());
    }

    private static ToolInvocationModel toToolInvocationModel(ToolInvocationLog toolInvocationLog) {
        return new ToolInvocationModel()
            .componentName(toolInvocationLog.getComponentName())
            .componentVersion(toolInvocationLog.getComponentVersion())
            .createdDate(toOffsetDateTime(toolInvocationLog.getCreatedDate()))
            .durationMs(toolInvocationLog.getDurationMs())
            .errorMessage(toolInvocationLog.getErrorMessage())
            .errorType(toolInvocationLog.getErrorType())
            .id(toolInvocationLog.getId())
            .integrationInstanceId(toolInvocationLog.getIntegrationInstanceId())
            .jobId(toolInvocationLog.getJobId())
            .kind(toToolInvocationKindModel(toolInvocationLog.getKind()))
            .operationName(toolInvocationLog.getOperationName())
            .outcome(toToolInvocationOutcomeModel(toolInvocationLog.getOutcome()))
            .surface(toToolInvocationSurfaceModel(toolInvocationLog.getSurface()))
            .toolName(toolInvocationLog.getToolName());
    }

    private static ToolInvocationPageModel toToolInvocationPageModel(Page<ToolInvocationLog> toolInvocationLogPage) {
        return new ToolInvocationPageModel()
            .content(
                toolInvocationLogPage.stream()
                    .map(ToolInvocationApiController::toToolInvocationModel)
                    .toList())
            .number(toolInvocationLogPage.getNumber())
            .numberOfElements(toolInvocationLogPage.getNumberOfElements())
            .size(toolInvocationLogPage.getSize())
            .totalElements(Math.toIntExact(toolInvocationLogPage.getTotalElements()))
            .totalPages(toolInvocationLogPage.getTotalPages());
    }

    private static @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
