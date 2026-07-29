/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import java.util.List;

/**
 * Deploys a plain automation code-workflow artifact into the embedded catalog.
 *
 * <p>
 * Unlike {@link IntegrationCodeWorkflowFacade}, which backs embedded-native integrations, this facade deploys a regular
 * automation {@code ProjectHandler} artifact and resolves/creates its backing catalog
 * {@code com.bytechef.automation.configuration.domain.Project} through {@link AutomationWorkflowProjectFacade}'s marker
 * convention, so the automation project stays hidden behind the embedded automation-workflow-project entity.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AutomationWorkflowProjectCodeWorkflowFacade {

    List<String> save(byte[] bytes, Language language);
}
