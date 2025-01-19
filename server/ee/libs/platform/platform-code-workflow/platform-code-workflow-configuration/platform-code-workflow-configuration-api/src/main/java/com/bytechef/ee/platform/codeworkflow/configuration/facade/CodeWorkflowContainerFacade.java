/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CodeWorkflowContainerFacade {

    CodeWorkflowContainer create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, ModeType type);
}
