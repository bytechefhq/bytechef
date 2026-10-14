/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationCodeWorkflowService {

    IntegrationCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Integration integration);

    Optional<IntegrationCodeWorkflow> fetchIntegrationCodeWorkflow(long integrationId);

    List<Long> getCodeWorkflowIntegrationIds();
}
