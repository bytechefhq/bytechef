/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectAdminFacade {

    void deleteProjectWorkflow(long connectedUserProjectWorkflowId);

    void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment);
}
