/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CopilotWorkflowGenerator {

    void generateWorkflow(String workflowId, String prompt, Set<String> allowedComponentNames);
}
