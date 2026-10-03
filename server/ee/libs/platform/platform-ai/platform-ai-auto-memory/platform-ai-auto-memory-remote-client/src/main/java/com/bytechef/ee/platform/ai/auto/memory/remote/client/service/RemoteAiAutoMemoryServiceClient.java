/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.auto.memory.remote.client.service;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Satisfies the {@link AiAutoMemoryService} dependency of the AI Agent Utils component in the microservice apps. Auto
 * memory is stored only by the server application, so no microservice can serve it: {@link #isAvailable()} is
 * {@code false}, which the memory tools report to the model, and every other call fails with
 * {@link UnsupportedOperationException}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAiAutoMemoryServiceClient implements AiAutoMemoryService {

    static final String UNSUPPORTED_MESSAGE =
        "Auto memory is not available in the microservices deployment; it requires the ByteChef server application.";

    @Override
    public AiAutoMemory create(
        AiAutoMemoryOwner owner, String name, String title, @Nullable String description,
        AiAutoMemoryType memoryType, String content) {

        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Optional<AiAutoMemory> read(AiAutoMemoryOwner owner, String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public AiAutoMemory update(
        AiAutoMemoryOwner owner, String name, long expectedVersion, AiAutoMemoryPatch patch) {

        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public AiAutoMemory updateById(
        AiAutoMemoryOwner owner, long memoryId, long expectedVersion, AiAutoMemoryPatch patch) {

        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public AiAutoMemory delete(AiAutoMemoryOwner owner, String name) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public AiAutoMemory deleteById(AiAutoMemoryOwner owner, long memoryId) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public AiAutoMemory rename(AiAutoMemoryOwner owner, String oldName, String newName) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public List<AiAutoMemory> list(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public List<AiAutoMemory> listAllOwners(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType) {

        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Optional<AiAutoMemory> findById(AiAutoMemoryOwner owner, long memoryId) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
