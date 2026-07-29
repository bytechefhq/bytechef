/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationCodeWorkflowFacade {

    Integration createEmptyCodeWorkflow(String componentName, Language language);

    List<Integration> getCodeWorkflowIntegrations();

    Optional<String> getCodeWorkflowLanguage(long integrationId);

    String getCodeWorkflowSource(long integrationId);

    void save(byte[] bytes, Language language);

    void updateCodeWorkflowSource(long integrationId, String content);
}
