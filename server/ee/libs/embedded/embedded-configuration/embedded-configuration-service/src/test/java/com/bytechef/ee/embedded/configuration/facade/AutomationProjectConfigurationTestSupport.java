/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AutomationProjectConfigurationTestSupport {

    private AutomationProjectConfigurationTestSupport() {
    }

    public static void publishAndPinProject(
        AutomationProjectFacade automationProjectFacade,
        AutomationProjectConfigurationService automationProjectConfigurationService, long projectId) {

        automationProjectFacade.publishProject(projectId);

        int publishedVersion = automationProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .map(AutomationProjectDTO::lastPublishedVersion)
            .findFirst()
            .orElseThrow();

        Optional<AutomationProjectConfiguration> configurationOptional =
            automationProjectConfigurationService.getConfigurations(
                projectId, Environment.PRODUCTION.ordinal())
                .stream()
                .findFirst();

        if (configurationOptional.isPresent()) {
            AutomationProjectConfiguration existingConfiguration = configurationOptional.get();

            automationProjectConfigurationService.update(
                existingConfiguration.getId(), existingConfiguration.getName(),
                existingConfiguration.getPermissionExpression(), publishedVersion, true, true);
        } else {
            automationProjectConfigurationService.create(projectId, Environment.PRODUCTION.ordinal(), "Default",
                null, publishedVersion, true, true);
        }
    }
}
