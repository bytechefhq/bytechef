/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowFacade {

    Project createEmptyCodeWorkflow(long workspaceId, String name, Language language);

    Project createEmptyCodeWorkflow(
        long workspaceId, String name, Language language, @Nullable String description, @Nullable Long categoryId,
        @Nullable List<String> tags);

    List<Project> getCodeWorkflowProjects();

    String getCodeWorkflowSource(long projectId);

    void save(long workspaceId, byte[] bytes, Language language);

    /**
     * Publishes the project like {@link #save(long, byte[], Language)} and, when {@code enable} is set, makes the
     * published version live in {@code environment}.
     */
    void save(long workspaceId, byte[] bytes, Language language, Environment environment, boolean enable);

    void updateCodeWorkflowSource(long projectId, String content);

    void deployCodeWorkflowSource(long projectId, Language language, String content);
}
