/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import org.jspecify.annotations.Nullable;

/**
 * A connected user assigned to an automation workflow project configuration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AutomationWorkflowProjectConfigurationConnectedUserDTO(
    long connectedUserId, String externalId, @Nullable String name, @Nullable String email) {
}
