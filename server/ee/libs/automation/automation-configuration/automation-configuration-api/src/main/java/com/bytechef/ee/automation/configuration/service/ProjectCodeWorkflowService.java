/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowService {

    ProjectCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Project projectCodeWorkflow);

    void deleteProjectCodeWorkflows(long projectId);

    List<Long> getCodeWorkflowProjectIds();

    ProjectCodeWorkflow getProjectCodeWorkflow(long projectId);
}
