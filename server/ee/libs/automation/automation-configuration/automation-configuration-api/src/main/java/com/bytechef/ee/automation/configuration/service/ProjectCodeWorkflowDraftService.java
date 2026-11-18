/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowDraftService {

    void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language);
}
