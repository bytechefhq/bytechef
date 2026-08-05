/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowFacade {

    Project createEmptyCodeWorkflow(long workspaceId, String name, Language language);

    /**
     * Creates a code-backed project together with the metadata the platform owns — the ones the source cannot declare —
     * so creation is one step rather than create-then-edit. Every metadata argument is optional.
     */
    Project createEmptyCodeWorkflow(
        long workspaceId, String name, Language language, @Nullable String description, @Nullable Long categoryId,
        @Nullable List<String> tags);

    List<Project> getCodeWorkflowProjects();

    String getCodeWorkflowSource(long projectId);

    void save(long workspaceId, byte[] bytes, Language language);

    void updateCodeWorkflowSource(long projectId, String content);

    /**
     * Deploys {@code content} into {@code projectId}, first rewriting the source's declared project name to the
     * project's own name. Used when a code workflow lands under a name it does not itself declare — a duplicate or an
     * imported project — where {@link #updateCodeWorkflowSource} would reject the mismatch.
     */
    void deployCodeWorkflowSource(long projectId, Language language, String content);
}
