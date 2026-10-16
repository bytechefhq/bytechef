/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowCopyDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserWorkflowReferenceAdminFacade {
    List<ConnectedUserWorkflowCopyDTO> getCopies(Set<String> automationWorkflowUuids, Environment environment);

    List<ConnectedUserWorkflowReferenceDTO> getReferences(Set<String> automationWorkflowUuids, Environment environment);
}
