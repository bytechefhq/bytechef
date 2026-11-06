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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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

        Page<ExternalInvocationLog> externalInvocationLogPage = toolInvocationFacade.getToolInvocations(
            externalUserId, environment, surface == null ? null : ExternalInvocationSurface.valueOf(surface.name()),
            outcome == null ? null : ExternalInvocationOutcome.valueOf(outcome.name()), toInstant(startDate),
            toInstant(endDate), pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(toToolInvocationPageModel(externalInvocationLogPage));
    }

    static ToolInvocationKindModel toToolInvocationKindModel(ExternalInvocationKind kind) {
        return ToolInvocationKindModel.valueOf(kind.name());
    }

    static ToolInvocationOutcomeModel toToolInvocationOutcomeModel(ExternalInvocationOutcome outcome) {
        return ToolInvocationOutcomeModel.valueOf(outcome.name());
    }

    static ToolInvocationSurfaceModel toToolInvocationSurfaceModel(ExternalInvocationSurface surface) {
        return ToolInvocationSurfaceModel.valueOf(surface.name());
    }

    private static ToolInvocationModel toToolInvocationModel(ExternalInvocationLog externalInvocationLog) {
        return new ToolInvocationModel()
            .componentName(externalInvocationLog.getComponentName())
            .componentVersion(externalInvocationLog.getComponentVersion())
            .createdDate(toOffsetDateTime(externalInvocationLog.getCreatedDate()))
            .durationMs(externalInvocationLog.getDurationMs())
            .errorMessage(externalInvocationLog.getErrorMessage())
            .errorType(externalInvocationLog.getErrorType())
            .id(externalInvocationLog.getId())
            .integrationInstanceId(externalInvocationLog.getIntegrationInstanceId())
            .jobId(externalInvocationLog.getJobId())
            .kind(toToolInvocationKindModel(externalInvocationLog.getKind()))
            .operationName(externalInvocationLog.getOperationName())
            .outcome(toToolInvocationOutcomeModel(externalInvocationLog.getOutcome()))
            .surface(toToolInvocationSurfaceModel(externalInvocationLog.getSurface()))
            .toolName(externalInvocationLog.getToolName());
    }

    private static ToolInvocationPageModel
        toToolInvocationPageModel(Page<ExternalInvocationLog> externalInvocationLogPage) {
        return new ToolInvocationPageModel()
            .content(
                externalInvocationLogPage.stream()
                    .map(ToolInvocationApiController::toToolInvocationModel)
                    .toList())
            .number(externalInvocationLogPage.getNumber())
            .numberOfElements(externalInvocationLogPage.getNumberOfElements())
            .size(externalInvocationLogPage.getSize())
            .totalElements(Math.toIntExact(externalInvocationLogPage.getTotalElements()))
            .totalPages(externalInvocationLogPage.getTotalPages());
    }

    private static @Nullable Instant toInstant(@Nullable OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toInstant();
    }

    private static @Nullable OffsetDateTime toOffsetDateTime(@Nullable Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
