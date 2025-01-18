/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.codeworkflow.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.codeworkflow.domain.ProjectCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowService {

    ProjectCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Project projectCodeWorkflow);
}
