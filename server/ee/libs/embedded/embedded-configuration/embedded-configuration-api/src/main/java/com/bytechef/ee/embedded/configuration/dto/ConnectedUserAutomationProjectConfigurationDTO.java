/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

/**
 * The automation workflow configuration a connected user is assigned to in one automation workflow project.
 *
 * @param projectId                   the automation workflow project id
 * @param configurationId             the assigned configuration id
 * @param configurationName           the assigned configuration name
 * @param configurationProjectVersion the project version the assigned configuration pins
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserAutomationProjectConfigurationDTO(
    long projectId, long configurationId, String configurationName, int configurationProjectVersion) {
}
