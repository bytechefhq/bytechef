/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.facade;

import com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService;
import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptService;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiPromptFacade}. Delegates to the shared {@link AiPromptService} /
 * {@link AiPromptVersionService} and the workspace-scoped {@link WorkspaceAiPromptService}, and carries the
 * authorization guards so they are enforced for every caller of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class AiPromptFacadeImpl implements AiPromptFacade {

    private final AiPromptService aiPromptService;
    private final AiPromptVersionService aiPromptVersionService;
    private final WorkspaceAiPromptService workspaceAiPromptService;

    @SuppressFBWarnings("EI")
    AiPromptFacadeImpl(
        AiPromptService aiPromptService, AiPromptVersionService aiPromptVersionService,
        WorkspaceAiPromptService workspaceAiPromptService) {

        this.aiPromptService = aiPromptService;
        this.aiPromptVersionService = aiPromptVersionService;
        this.workspaceAiPromptService = workspaceAiPromptService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt getPrompt(long id) {
        return aiPromptService.getPrompt(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiPrompt> getPromptsByWorkspace(Long workspaceId) {
        return workspaceAiPromptService.getPromptsByWorkspace(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiPromptVersion> getVersionsByPrompt(Long promptId) {
        return aiPromptVersionService.getVersionsByPrompt(promptId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId) {
        return workspaceAiPromptService.createInWorkspace(prompt, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public int getNextVersionNumber(Long promptId) {
        return aiPromptVersionService.getNextVersionNumber(promptId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPromptVersion createVersion(AiPromptVersion promptVersion) {
        return aiPromptVersionService.create(promptVersion);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteInWorkspace(long promptId) {
        workspaceAiPromptService.deleteInWorkspace(promptId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void setActiveVersion(long promptVersionId, String environment) {
        aiPromptVersionService.setActiveVersion(promptVersionId, environment);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiPrompt update(AiPrompt prompt) {
        return aiPromptService.update(prompt);
    }
}
