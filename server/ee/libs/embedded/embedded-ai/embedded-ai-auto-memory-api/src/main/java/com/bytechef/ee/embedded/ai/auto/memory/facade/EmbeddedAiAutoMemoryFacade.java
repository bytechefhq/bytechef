/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.facade;

import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Admin access to the memories AI agents save while running embedded integrations and automations. Each memory is owned
 * by an integration instance or by an Embedded-owned project deployment, and through it by a connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedAiAutoMemoryFacade {

    void deleteEmbeddedAiAutoMemory(
        Environment environment, AiAutoMemoryPrincipalType principalType, long principalId, long id);

    List<EmbeddedAiAutoMemory> getEmbeddedAiAutoMemories(
        Environment environment, @Nullable Long connectedUserId, @Nullable AiAutoMemoryType memoryType);

    List<EmbeddedAiAutoMemoryConnectedUser> getEmbeddedAiAutoMemoryConnectedUsers(Environment environment);

    EmbeddedAiAutoMemory updateEmbeddedAiAutoMemory(
        Environment environment, AiAutoMemoryPrincipalType principalType, long principalId, long id,
        long expectedVersion, AiAutoMemoryPatch patch);
}
