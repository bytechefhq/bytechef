/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserCodeWorkflowReferenceDTO;
import java.util.List;
import java.util.Set;

/**
 * Admin-only read seam over automation-bridge references, keyed by catalog workflow rather than by connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserCodeWorkflowReferenceAdminFacade {

    List<ConnectedUserCodeWorkflowReferenceDTO> getReferences(Set<String> catalogWorkflowUuids);
}
