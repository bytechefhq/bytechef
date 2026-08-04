/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * EE implementation of the CE {@link WorkspaceSystemPromptAdvisorProvider} SPI: resolves the calling run's workspace
 * ({@code jobPrincipalId} → project deployment → project → workspace, the same memoized chain as
 * {@code AiGuardrailsAdvisorProviderImpl}) and, when that workspace has a prompt set, returns a
 * {@link WorkspaceSystemPromptAdvisor} bound to it. Unlike guardrails there is no tenant default: a non-AUTOMATION run,
 * an unknown principal, or a failed resolution yields empty — no advisor — rather than a fallback policy.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceSystemPromptAdvisorProviderImpl implements WorkspaceSystemPromptAdvisorProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSystemPromptAdvisorProviderImpl.class);

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private record WorkspaceCacheKey(PlatformType platformType, long jobPrincipalId) {
    }

    private final WorkspaceSystemPrompts workspaceSystemPrompts;
    private final ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider;
    private final ObjectProvider<ProjectService> projectServiceProvider;
    private final Cache<WorkspaceCacheKey, Optional<Long>> workspaceIdCache = Caffeine.newBuilder()
        .expireAfterWrite(CACHE_TTL)
        .build();

    @SuppressFBWarnings("EI2")
    public WorkspaceSystemPromptAdvisorProviderImpl(
        WorkspaceSystemPrompts workspaceSystemPrompts,
        ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider,
        ObjectProvider<ProjectService> projectServiceProvider) {

        this.workspaceSystemPrompts = workspaceSystemPrompts;
        this.projectDeploymentServiceProvider = projectDeploymentServiceProvider;
        this.projectServiceProvider = projectServiceProvider;
    }

    @Override
    public Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        Long workspaceId = resolveWorkspaceId(platformType, jobPrincipalId);

        if (workspaceId == null || workspaceSystemPrompts.fetchPrompt(workspaceId) == null) {
            return Optional.empty();
        }

        return Optional.of(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
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
                "ProjectDeploymentService/ProjectService not available; no workspace system prompt for job principal "
                    + "{}",
                jobPrincipalId);

            return null;
        }

        try {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);
            Project project = projectService.getProject(projectDeployment.getProjectId());

            return project.getWorkspaceId();
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to resolve workspace for job principal {}; skipping workspace system prompt",
                jobPrincipalId, exception);

            return null;
        }
    }
}
