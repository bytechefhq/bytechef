/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EmbeddedGettingStartedDTO(
    boolean apiKeyCreated, boolean integrationCreated, boolean integrationHasWorkflows, boolean integrationPublished,
    boolean integrationConfigurationCreated, boolean integrationConfigurationEnabled, boolean testUserAdded,
    boolean testUserConnected, @Nullable String testUserExternalId, boolean firstCallMade,
    @Nullable LastFailedCallDTO lastFailedCall, boolean completed, @Nullable String completionType,
    @Nullable Long integrationId, @Nullable Long integrationInstanceConfigurationId, @Nullable Long testUserId,
    @Nullable String componentName, @Nullable Integer componentVersion) {
}
