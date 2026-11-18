/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.domain;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import org.jspecify.annotations.Nullable;

/**
 * A memory owned by an integration instance, with the connected user and integration of that instance. The connected
 * user and integration fields are null when the instance, its connected user or its integration no longer exists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EmbeddedAiAutoMemory(
    AiAutoMemory memory, @Nullable Long connectedUserId, @Nullable String connectedUserName,
    @Nullable String connectedUserEmail, @Nullable String connectedUserExternalId, @Nullable Long integrationId,
    @Nullable String integrationName, @Nullable String integrationComponentName) {
}
