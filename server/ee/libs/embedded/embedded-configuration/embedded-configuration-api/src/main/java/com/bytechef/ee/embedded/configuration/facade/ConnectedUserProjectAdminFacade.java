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
 * Admin-console-only facade over {@link ConnectedUserProjectFacade}. The underlying facade is shared with the public
 * {@code /v1} API and workflow-execution components (and carries {@code @SkipAutomationAuthorization}), so it cannot
 * itself carry an admin gate. This per-controller facade wraps the admin operations behind {@code isTenantAdmin()} so
 * the connected-user-project GraphQL controller needs no controller-layer authorization.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectAdminFacade {

    void deleteProjectWorkflow(long connectedUserProjectWorkflowId);

    void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable);

    List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment);
}
