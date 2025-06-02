/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectUserProjectWorkflowDTO;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectUserProjectFacade {

    String createProjectWorkflow(
        String definition, Environment environment);

    void deleteProjectWorkflow(String workflowReferenceCode, Environment environment);

    void enableProjectWorkflow(Environment environment, String workflowReferenceCode, boolean enable);

    ConnectUserProjectWorkflowDTO getProjectWorkflow(String workflowReferenceCode, Environment environment);

    List<ConnectUserProjectWorkflowDTO> getProjectWorkflows(Environment environment);

    void publishProjectWorkflow(String workflowReferenceCode, Environment environment);

    void updateProjectWorkflow(String workflowReferenceCode, String definition, Environment environment);
}
