/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.domain;

import org.jspecify.annotations.Nullable;

/**
 * A connected user that owns at least one embedded memory, with how many it owns.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EmbeddedAiAutoMemoryConnectedUser(
    long id, @Nullable String name, @Nullable String email, String externalId, int memoryCount) {
}
