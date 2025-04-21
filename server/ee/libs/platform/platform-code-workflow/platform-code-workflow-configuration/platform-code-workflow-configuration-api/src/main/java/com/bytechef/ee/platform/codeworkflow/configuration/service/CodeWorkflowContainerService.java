/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.service;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CodeWorkflowContainerService {

    CodeWorkflowContainer create(CodeWorkflowContainer codeWorkflowContainer);

    CodeWorkflowContainer getCodeWorkflowContainer(String codeWorkflowContainerReference);
}
