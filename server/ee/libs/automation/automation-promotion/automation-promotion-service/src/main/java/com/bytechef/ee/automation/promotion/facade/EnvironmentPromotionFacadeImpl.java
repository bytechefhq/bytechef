/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.facade;

import com.bytechef.ee.automation.promotion.audit.EnvironmentPromotionAuditEvents;
import com.bytechef.ee.automation.promotion.audit.EnvironmentPromotionAuditMapper;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EnvironmentPromotionFacadeImpl implements EnvironmentPromotionFacade {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentPromotionFacadeImpl.class);

    private final Map<String, EnvironmentPromotionContributor> contributors;
    private final EnvironmentService environmentService;
    private final @Nullable MeterRegistry meterRegistry;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public EnvironmentPromotionFacadeImpl(
        EnvironmentService environmentService, List<EnvironmentPromotionContributor> contributors,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.contributors = contributorsByResourceType(contributors);
        this.environmentService = environmentService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public EnvironmentPromotionPreview preview(
        String resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId) {

        EnvironmentPromotionContributor contributor = contributor(resourceType);
        Environment sourceEnvironment = availableEnvironment(sourceEnvironmentId);
        Environment targetEnvironment = availableEnvironment(targetEnvironmentId);

        checkDifferentEnvironments(sourceEnvironment, targetEnvironment);

        return contributor.preview(sourceId, sourceEnvironment, targetEnvironment);
    }

    @Override
    @Audited(
        event = EnvironmentPromotionAuditEvents.ENVIRONMENT_PROMOTED, mapper = EnvironmentPromotionAuditMapper.class,
        data = {
            @AuditData(key = "resourceType", value = "#resourceType"),
            @AuditData(key = "sourceId", value = "#sourceId"),
            @AuditData(key = "sourceEnvironmentId", value = "#sourceEnvironmentId"),
            @AuditData(key = "targetEnvironmentId", value = "#targetEnvironmentId"),
            @AuditData(key = "targetId", value = "#result?.targetId"),
            @AuditData(key = "created", value = "#result?.created")
        })
    public EnvironmentPromotionResult promote(
        String resourceType, long sourceId, long sourceEnvironmentId, long targetEnvironmentId,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

        EnvironmentPromotionContributor contributor = contributor(resourceType);
        Environment sourceEnvironment = availableEnvironment(sourceEnvironmentId);
        Environment targetEnvironment = availableEnvironment(targetEnvironmentId);

        checkDifferentEnvironments(sourceEnvironment, targetEnvironment);

        try {
            EnvironmentPromotionResult result = contributor.promote(
                sourceId, sourceEnvironment, targetEnvironment, connectionMappings,
                expectedDestructiveChangesHash);

            record(resourceType, result.created() ? "created" : "updated");

            List<Long> unresolvedConnectionIds = result.unresolvedConnectionIds();

            log.info(
                "Promoted {} id={} from {} to {} (targetId={}, created={}, unresolvedConnections={})", resourceType,
                sourceId, sourceEnvironment, targetEnvironment, result.targetId(), result.created(),
                unresolvedConnectionIds.size());

            return result;
        } catch (RuntimeException runtimeException) {
            record(resourceType, "failed");

            log.warn(
                "Promoting {} id={} from {} to {} failed", resourceType, sourceId, sourceEnvironment, targetEnvironment,
                runtimeException);

            throw runtimeException;
        }
    }

    private static Map<String, EnvironmentPromotionContributor> contributorsByResourceType(
        List<EnvironmentPromotionContributor> contributors) {

        Map<String, EnvironmentPromotionContributor> contributorsByResourceType = new HashMap<>();

        for (EnvironmentPromotionContributor contributor : contributors) {
            String resourceType = contributor.getResourceType();

            EnvironmentPromotionContributor registeredContributor =
                contributorsByResourceType.putIfAbsent(resourceType, contributor);

            if (registeredContributor != null) {
                Class<?> registeredContributorClass = registeredContributor.getClass();
                Class<?> contributorClass = contributor.getClass();

                throw new IllegalStateException(
                    "Promotion contributors %s and %s both register resource type %s".formatted(
                        registeredContributorClass.getName(), contributorClass.getName(), resourceType));
            }
        }

        return Map.copyOf(contributorsByResourceType);
    }

    private static void checkDifferentEnvironments(Environment sourceEnvironment, Environment targetEnvironment) {
        if (sourceEnvironment == targetEnvironment) {
            throw new ConfigurationException(
                "Source and target environment are both %s".formatted(targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }
    }

    private EnvironmentPromotionContributor contributor(String resourceType) {
        EnvironmentPromotionContributor contributor = contributors.get(resourceType);

        if (contributor == null) {
            throw new ConfigurationException(
                "No promotion contributor registered for resource type %s".formatted(resourceType),
                EnvironmentPromotionErrorType.UNSUPPORTED_RESOURCE_TYPE);
        }

        return contributor;
    }

    private Environment availableEnvironment(long environmentId) {
        Environment environment;

        try {
            environment = environmentService.getEnvironment(environmentId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                "Environment id %d is not available".formatted(environmentId),
                EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE);
        }

        List<Environment> availableEnvironments = environmentService.getEnvironments();

        if (!availableEnvironments.contains(environment)) {
            throw new ConfigurationException(
                "Environment %s is not available".formatted(environment),
                EnvironmentPromotionErrorType.ENVIRONMENT_NOT_AVAILABLE);
        }

        return environment;
    }

    private void record(String resourceType, String outcome) {
        if (meterRegistry == null) {
            return;
        }

        Counter counter = meterRegistry.counter(
            "bytechef_environment_promotion", "resource", resourceType.toLowerCase(Locale.ROOT), "outcome", outcome);

        counter.increment();
    }
}
