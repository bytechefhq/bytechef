/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A published automation workflow project as one configuration releases it to a connected user.
 *
 * @param configurationId   the configuration id
 * @param configurationName the configuration name
 * @param project           the project, with the configuration's version and the workflow templates it releases to the
 *                          connected user
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationReleaseDTO(
    long configurationId, String configurationName, AutomationWorkflowProjectDTO project) {
}
