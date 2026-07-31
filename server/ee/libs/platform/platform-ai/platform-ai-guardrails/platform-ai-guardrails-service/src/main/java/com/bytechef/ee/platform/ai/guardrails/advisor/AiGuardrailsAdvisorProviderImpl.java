/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * EE implementation of the CE {@link AiGuardrailsAdvisorProvider} SPI: resolves the calling run's workspace and, when
 * at least one guardrail is active for it, returns an {@link AiGuardrailsAdvisor} bound to that workspace.
 *
 * <p>
 * Workspace resolution only applies to {@link PlatformType#AUTOMATION} runs with a non-null {@code jobPrincipalId}
 * (interpreted as a {@link ProjectDeployment} id, the same principal id
 * {@code WorkflowExecutionCostApplicationEventListener} resolves from): {@code jobPrincipalId} →
 * {@link ProjectDeploymentService#getProjectDeployment(long)} → {@link ProjectDeployment#getProjectId()} →
 * {@link ProjectService#getProject(long)} → {@link Project#getWorkspaceId()}. Embedded runs, a {@code null}
 * {@code jobPrincipalId}, or any resolution failure (deleted deployment/project, transient lookup error) resolve to
 * {@code workspaceId = null} — the tenant default — rather than skipping guardrails: only the workspace SCOPE is
 * fail-open here, guardrails themselves are never silently skipped because attribution failed.
 * </p>
 *
 * <p>
 * {@link ProjectDeploymentService} and {@link ProjectService} are optional (injected via {@link ObjectProvider}) rather
 * than hard constructor dependencies: this module is on the classpath of EE apps (e.g. ai-gateway-app, ai-copilot-app)
 * that carry neither {@code automation-configuration-service} nor its remote-client stubs. When either provider has no
 * bean to return, workspace resolution degrades the same way a resolution failure does — {@code workspaceId = null},
 * the tenant default — consistent with this class's existing fail-open contract.
 * </p>
 *
 * <p>
 * Resolution runs on every model call in an agent loop, so the (platformType, jobPrincipalId) → workspaceId lookup is
 * memoized in a short-lived Caffeine cache.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnBean(AiGuardrails.class)
public class AiGuardrailsAdvisorProviderImpl implements AiGuardrailsAdvisorProvider {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailsAdvisorProviderImpl.class);

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private record WorkspaceCacheKey(PlatformType platformType, long jobPrincipalId) {
    }

    private final AiGuardrails aiGuardrails;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider;
    private final ObjectProvider<ProjectService> projectServiceProvider;
    private final Cache<WorkspaceCacheKey, Optional<Long>> workspaceIdCache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .build();

    @SuppressFBWarnings("EI2")
    public AiGuardrailsAdvisorProviderImpl(
        AiGuardrails aiGuardrails, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider,
        ObjectProvider<ProjectService> projectServiceProvider) {

        this.aiGuardrails = aiGuardrails;
        this.meterRegistryProvider = meterRegistryProvider;
        this.projectDeploymentServiceProvider = projectDeploymentServiceProvider;
        this.projectServiceProvider = projectServiceProvider;
    }

    @Override
    public Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        Long workspaceId = resolveWorkspaceId(platformType, jobPrincipalId);

        if (!aiGuardrails.isActive(workspaceId)) {
            return Optional.empty();
        }

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistryProvider.getIfAvailable(), surface);

        return Optional.of(new AiGuardrailsAdvisor(aiGuardrails, workspaceId, metrics));
    }

    private @Nullable Long resolveWorkspaceId(@Nullable PlatformType platformType, @Nullable Long jobPrincipalId) {
        if (platformType != PlatformType.AUTOMATION || jobPrincipalId == null) {
            return null;
        }

        WorkspaceCacheKey cacheKey = new WorkspaceCacheKey(platformType, jobPrincipalId);

        Optional<Long> cachedWorkspaceId = workspaceIdCache.get(
            cacheKey, key -> Optional.ofNullable(fetchWorkspaceId(key.jobPrincipalId())));

        return cachedWorkspaceId.orElse(null);
    }

    private @Nullable Long fetchWorkspaceId(long jobPrincipalId) {
        ProjectDeploymentService projectDeploymentService = projectDeploymentServiceProvider.getIfAvailable();
        ProjectService projectService = projectServiceProvider.getIfAvailable();

        if (projectDeploymentService == null || projectService == null) {
            log.debug(
                "ProjectDeploymentService/ProjectService not available; applying tenant-default guardrails for job principal {}",
                jobPrincipalId);

            return null;
        }

        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);
            Project project = projectService.getProject(projectDeployment.getProjectId());

            return project.getWorkspaceId();
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve workspace for job principal {}; applying tenant-default guardrails",
                jobPrincipalId, exception);

            return null;
        }
    }
}
