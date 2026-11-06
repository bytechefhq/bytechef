/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class WorkspaceSystemPromptAdvisorProviderImpl implements WorkspaceSystemPromptAdvisorProvider {

    private final WorkspaceSystemPrompts workspaceSystemPrompts;
    private final JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver;

    @SuppressFBWarnings("EI2")
    public WorkspaceSystemPromptAdvisorProviderImpl(
        WorkspaceSystemPrompts workspaceSystemPrompts,
        JobPrincipalWorkspaceResolver jobPrincipalWorkspaceResolver) {

        this.workspaceSystemPrompts = workspaceSystemPrompts;
        this.jobPrincipalWorkspaceResolver = jobPrincipalWorkspaceResolver;
    }

    @Override
    public Optional<Advisor> getAdvisor(
        @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

        Long workspaceId = jobPrincipalWorkspaceResolver.resolve(platformType, jobPrincipalId);

        if (workspaceId == null || workspaceSystemPrompts.fetchPrompt(workspaceId) == null) {
            return Optional.empty();
        }

        return Optional.of(new WorkspaceSystemPromptAdvisor(workspaceSystemPrompts, workspaceId));
    }
}
