/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.domain;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

/**
 * A memory owned by an integration instance or by an Embedded-owned project deployment, with its connected user and
 * source. An integration instance's memory carries its integration; a deployment's memory carries the automation the
 * connected user enabled, or no automation when the deployment holds the connected user's own workflows. The connected
 * user and source fields are null when the owner, its connected user or its integration no longer exists.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record EmbeddedAiAutoMemory(
    AiAutoMemory memory, @Nullable Long connectedUserId, @Nullable String connectedUserName,
    @Nullable String connectedUserEmail, @Nullable String connectedUserExternalId, @Nullable Long integrationId,
    @Nullable String integrationName, @Nullable String integrationComponentName, @Nullable Long automationId,
    @Nullable String automationName) {
}
