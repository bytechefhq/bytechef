/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

/**
 * What deleting a configuration removes: its connected users' assignments and their reference workflows.
 *
 * @param connectedUserCount     the number of connected users assigned to the configuration
 * @param referenceWorkflowCount the number of reference workflows of those users that the delete removes
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AutomationWorkflowProjectConfigurationDeleteImpactDTO(int connectedUserCount,
    int referenceWorkflowCount) {
}
