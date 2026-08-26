/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.handler.EnvironmentPromotionHandler;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Dispatches {@link #preview} and {@link #promote} calls to the {@link EnvironmentPromotionHandler} registered for the
 * requested {@link PromotionResourceType}, after resolving and validating the target environment.
 *
 * <p>
 * Handlers are collected into a resource-type-keyed map at construction time via
 * {@link Collectors#toUnmodifiableMap(java.util.function.Function, java.util.function.Function)}, which has no merge
 * function and therefore throws {@link IllegalStateException} if two handler beans claim the same
 * {@link PromotionResourceType} — a deliberate fail-fast at application startup rather than a silent last-wins
 * registration that would make one handler unreachable without any signal. An empty (or partial) handler list is not an
 * error here: it simply makes every unregistered {@link PromotionResourceType} report
 * {@link EnvironmentPromotionErrorType#UNSUPPORTED_RESOURCE_TYPE} at call time, which is the expected state before
 * Tasks 9-11/21 register their handlers.
 * </p>
 *
 * <p>
 * This facade never checks authorization itself; see {@link EnvironmentPromotionHandler} for where those guards live.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EnvironmentPromotionFacadeImpl implements EnvironmentPromotionFacade {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPromotionFacadeImpl.class);

    private final EnvironmentService environmentService;
    private final Map<PromotionResourceType, EnvironmentPromotionHandler> handlers;
    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public EnvironmentPromotionFacadeImpl(
        EnvironmentService environmentService, List<EnvironmentPromotionHandler> handlers,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.environmentService = environmentService;
        this.handlers = handlers.stream()
            .collect(Collectors.toUnmodifiableMap(EnvironmentPromotionHandler::getResourceType, handler -> handler));
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public EnvironmentPromotionPreview preview(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId) {

        return handler(resourceType).preview(sourceId, targetEnvironment(targetEnvironmentId));
    }

    @Override
    public EnvironmentPromotionResult promote(
        PromotionResourceType resourceType, long sourceId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings) {

        EnvironmentPromotionHandler handler = handler(resourceType);
        Environment targetEnvironment = targetEnvironment(targetEnvironmentId);

        try {
            EnvironmentPromotionResult result = handler.promote(sourceId, targetEnvironment, connectionMappings);

            record(resourceType, result.created() ? "created" : "updated");

            log.info(
                "Promoted {} id={} to {} (targetId={}, created={}, unresolvedConnections={})", resourceType, sourceId,
                targetEnvironment, result.targetId(), result.created(), result.unresolvedConnectionIds()
                    .size());

            return result;
        } catch (RuntimeException runtimeException) {
            record(resourceType, "failed");

            throw runtimeException;
        }
    }

    private EnvironmentPromotionHandler handler(PromotionResourceType resourceType) {
        EnvironmentPromotionHandler handler = handlers.get(resourceType);

        if (handler == null) {
            throw new ConfigurationException(
                "No promotion handler registered for resource type %s".formatted(resourceType),
                EnvironmentPromotionErrorType.UNSUPPORTED_RESOURCE_TYPE);
        }

        return handler;
    }

    private Environment targetEnvironment(long targetEnvironmentId) {
        Environment environment = environmentService.getEnvironment(targetEnvironmentId);

        if (!environmentService.getEnvironments()
            .contains(environment)) {

            throw new ConfigurationException(
                "Environment %s is not available".formatted(environment),
                EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE);
        }

        return environment;
    }

    private void record(PromotionResourceType resourceType, String outcome) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
            "bytechef_environment_promotion", "resource", resourceType.name()
                .toLowerCase(Locale.ROOT),
            "outcome", outcome)
            .increment();
    }
}
