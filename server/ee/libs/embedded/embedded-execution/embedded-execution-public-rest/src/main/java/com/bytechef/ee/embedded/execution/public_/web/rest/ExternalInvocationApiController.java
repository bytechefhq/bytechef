/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.execution.facade.ExternalInvocationFacade;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationOutcomeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationPageModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationSurfaceModel;
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
public class ExternalInvocationApiController implements ExternalInvocationApi {

    private final EnvironmentService environmentService;
    private final ExternalInvocationFacade externalInvocationFacade;

    @SuppressFBWarnings("EI")
    public ExternalInvocationApiController(
        EnvironmentService environmentService, ExternalInvocationFacade externalInvocationFacade) {

        this.environmentService = environmentService;
        this.externalInvocationFacade = externalInvocationFacade;
    }

    @Override
    public ResponseEntity<ExternalInvocationPageModel> getExternalInvocationsPage(
        String externalUserId, @Nullable EnvironmentModel xEnvironment,
        @Nullable ExternalInvocationSurfaceModel surface,
        @Nullable ExternalInvocationOutcomeModel outcome, @Nullable OffsetDateTime startDate,
        @Nullable OffsetDateTime endDate, @Nullable Integer pageNumber) {

        Environment environment = environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());

        Page<ExternalInvocationLog> externalInvocationLogPage = externalInvocationFacade.getExternalInvocations(
            externalUserId, environment, surface == null ? null : ExternalInvocationSurface.valueOf(surface.name()),
            outcome == null ? null : ExternalInvocationOutcome.valueOf(outcome.name()), toInstant(startDate),
            toInstant(endDate), pageNumber == null ? 0 : pageNumber);

        return ResponseEntity.ok(toExternalInvocationPageModel(externalInvocationLogPage));
    }

    static ExternalInvocationKindModel toExternalInvocationKindModel(ExternalInvocationKind kind) {
        return ExternalInvocationKindModel.valueOf(kind.name());
    }

    static ExternalInvocationOutcomeModel toExternalInvocationOutcomeModel(ExternalInvocationOutcome outcome) {
        return ExternalInvocationOutcomeModel.valueOf(outcome.name());
    }

    static ExternalInvocationSurfaceModel toExternalInvocationSurfaceModel(ExternalInvocationSurface surface) {
        return ExternalInvocationSurfaceModel.valueOf(surface.name());
    }

    private static ExternalInvocationModel toExternalInvocationModel(ExternalInvocationLog externalInvocationLog) {
        return new ExternalInvocationModel()
            .componentName(externalInvocationLog.getComponentName())
            .componentVersion(externalInvocationLog.getComponentVersion())
            .createdDate(toOffsetDateTime(externalInvocationLog.getCreatedDate()))
            .durationMs(externalInvocationLog.getDurationMs())
            .errorMessage(externalInvocationLog.getErrorMessage())
            .errorType(externalInvocationLog.getErrorType())
            .id(externalInvocationLog.getId())
            .integrationInstanceId(externalInvocationLog.getIntegrationInstanceId())
            .jobId(externalInvocationLog.getJobId())
            .kind(toExternalInvocationKindModel(externalInvocationLog.getKind()))
            .operationName(externalInvocationLog.getOperationName())
            .outcome(toExternalInvocationOutcomeModel(externalInvocationLog.getOutcome()))
            .surface(toExternalInvocationSurfaceModel(externalInvocationLog.getSurface()))
            .toolName(externalInvocationLog.getToolName());
    }

    private static ExternalInvocationPageModel
        toExternalInvocationPageModel(Page<ExternalInvocationLog> externalInvocationLogPage) {
        return new ExternalInvocationPageModel()
            .content(
                externalInvocationLogPage.stream()
                    .map(ExternalInvocationApiController::toExternalInvocationModel)
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
