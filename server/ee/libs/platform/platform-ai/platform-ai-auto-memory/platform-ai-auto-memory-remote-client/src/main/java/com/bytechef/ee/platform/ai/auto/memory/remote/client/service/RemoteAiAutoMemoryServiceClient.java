/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.auto.memory.remote.client.service;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAiAutoMemoryServiceClient implements AiAutoMemoryService {

    @Override
    public AiAutoMemory create(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name,
        String title, @Nullable String description, AiAutoMemoryType memoryType, String content) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AiAutoMemory> read(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AiAutoMemory update(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name,
        @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType,
        @Nullable String content) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AiAutoMemory updateById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId,
        @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType,
        @Nullable String content) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AiAutoMemory delete(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AiAutoMemory deleteById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AiAutoMemory rename(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String oldName,
        String newName) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiAutoMemory> list(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
        @Nullable AiAutoMemoryType memoryType) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AiAutoMemory> findById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiAutoMemory> listByPrincipalAndWorkspace(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment) {

        throw new UnsupportedOperationException();
    }
}
