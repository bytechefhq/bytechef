/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserWorkflowReferenceAdminFacade {
    List<ConnectedUserWorkflowReferenceDTO> getReferences(Set<String> automationWorkflowUuids);
}
