/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.auto.memory;

import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link AiAutoMemoryService}. Every per-owner lookup goes through the repository's
 * owner-scoped finders with the caller's {@link AiAutoMemoryOwner}, so a row is reachable only through the exact
 * workspace, principal and environment it was created for.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAutoMemoryServiceImpl implements AiAutoMemoryService {

    private static final Logger log = LoggerFactory.getLogger(AiAutoMemoryServiceImpl.class);

    private final AiAutoMemoryRepository aiMemoryRepository;
    private final Clock clock;

    @Autowired
    public AiAutoMemoryServiceImpl(AiAutoMemoryRepository aiMemoryRepository) {
        this(aiMemoryRepository, Clock.systemUTC());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAutoMemoryServiceImpl(AiAutoMemoryRepository aiMemoryRepository, Clock clock) {
        this.aiMemoryRepository = aiMemoryRepository;
        this.clock = clock;
    }

    @Override
    public AiAutoMemory create(
        AiAutoMemoryOwner owner, String name, String title, @Nullable String description,
        AiAutoMemoryType memoryType, String content) {

        validateName(name);
        validateRequired(title, "title");
        validateRequired(content, "content");

        if (memoryType == null) {
            throw new IllegalArgumentException("memoryType is required");
        }

        if (!findAllByName(owner, name).isEmpty()) {
            throw new DuplicateAiAutoMemoryNameException(name);
        }

        LocalDateTime now = LocalDateTime.now(clock);

        AiAutoMemory memory = new AiAutoMemory(owner);

        memory.setName(name);
        memory.setTitle(title);
        memory.setDescription(blankToNull(description));
        memory.setMemoryType(memoryType);
        memory.setContent(content);
        memory.setCreatedAt(now);
        memory.setUpdatedAt(now);

        return saveUniquelyNamed(memory, name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAutoMemory> read(AiAutoMemoryOwner owner, String name) {
        List<AiAutoMemory> matches = findAllByName(owner, name);

        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    @Override
    public AiAutoMemory update(
        AiAutoMemoryOwner owner, String name, long expectedVersion, AiAutoMemoryPatch patch) {

        Objects.requireNonNull(patch, "patch");

        AiAutoMemory memory = loadByName(owner, name);

        verifyExpectedVersion(memory, expectedVersion);

        applyPatch(memory, patch);

        return saveExisting(memory);
    }

    @Override
    public AiAutoMemory updateById(
        AiAutoMemoryOwner owner, long memoryId, long expectedVersion, AiAutoMemoryPatch patch) {

        Objects.requireNonNull(patch, "patch");

        AiAutoMemory memory = loadAndCheckOwnership(owner, memoryId);

        verifyExpectedVersion(memory, expectedVersion);

        applyPatch(memory, patch);

        return saveExisting(memory);
    }

    @Override
    public AiAutoMemory delete(AiAutoMemoryOwner owner, String name) {
        AiAutoMemory memory = loadByName(owner, name);

        deleteExisting(memory);

        return memory;
    }

    @Override
    public AiAutoMemory deleteById(AiAutoMemoryOwner owner, long memoryId) {
        AiAutoMemory memory = loadAndCheckOwnership(owner, memoryId);

        deleteExisting(memory);

        return memory;
    }

    @Override
    public AiAutoMemory rename(AiAutoMemoryOwner owner, String oldName, String newName) {
        validateName(newName);

        if (oldName.equals(newName)) {
            return loadByName(owner, oldName);
        }

        if (!findAllByName(owner, newName).isEmpty()) {
            throw new DuplicateAiAutoMemoryNameException(newName);
        }

        AiAutoMemory memory = loadByName(owner, oldName);

        memory.setName(newName);
        memory.setUpdatedAt(LocalDateTime.now(clock));

        return saveUniquelyNamed(memory, newName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAutoMemory> list(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType) {
        return aiMemoryRepository.findByOwner(owner, memoryType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAutoMemory> listAllOwners(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType) {

        return aiMemoryRepository.findByWorkspace(workspaceId, environment, memoryType);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAutoMemory> findById(AiAutoMemoryOwner owner, long memoryId) {
        return aiMemoryRepository.findOwnedById(owner, memoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment) {
        return aiMemoryRepository.listPrincipals(workspaceId, environment);
    }

    /**
     * The lookup before this call gives the caller a friendly error, but two concurrent writers can both pass it. The
     * JDBC backend's unique constraint on (workspace, principal type, principal id, environment, name) decides that
     * race; on file storage both writers can succeed. A rename of a memory another writer changed meanwhile fails the
     * optimistic lock.
     */
    private AiAutoMemory saveUniquelyNamed(AiAutoMemory memory, String name) {
        try {
            return aiMemoryRepository.save(memory);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new DuplicateAiAutoMemoryNameException(name, duplicateKeyException);
        } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
            throw new AiAutoMemoryConcurrentModificationException(name, optimisticLockingFailureException);
        }
    }

    /**
     * The version check before this call only covers what the caller read; the repository's optimistic lock covers a
     * write that lands between this service's own load and save.
     */
    private AiAutoMemory saveExisting(AiAutoMemory memory) {
        try {
            return aiMemoryRepository.save(memory);
        } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
            throw new AiAutoMemoryConcurrentModificationException(memory.getName(), optimisticLockingFailureException);
        }
    }

    private void deleteExisting(AiAutoMemory memory) {
        try {
            aiMemoryRepository.delete(memory);
        } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
            throw new AiAutoMemoryConcurrentModificationException(memory.getName(), optimisticLockingFailureException);
        }
    }

    private List<AiAutoMemory> findAllByName(AiAutoMemoryOwner owner, String name) {
        List<AiAutoMemory> matches = aiMemoryRepository.findAllByOwnerAndName(owner, name);

        if (matches.size() > 1) {
            log.warn(
                "Found {} memories named '{}' for one owner; using the first match. Only the JDBC binding "
                    + "enforces unique names.",
                matches.size(), name);
        }

        return matches;
    }

    private AiAutoMemory loadByName(AiAutoMemoryOwner owner, String name) {
        List<AiAutoMemory> matches = findAllByName(owner, name);

        if (matches.isEmpty()) {
            throw new AiAutoMemoryNotFoundException("Memory '" + name + "' not found");
        }

        return matches.getFirst();
    }

    private void applyPatch(AiAutoMemory memory, AiAutoMemoryPatch patch) {
        String title = patch.title();
        String description = patch.description();
        AiAutoMemoryType memoryType = patch.memoryType();
        String content = patch.content();

        if (title != null) {
            validateRequired(title, "title");

            memory.setTitle(title);
        }

        if (description != null) {
            memory.setDescription(blankToNull(description));
        }

        if (memoryType != null) {
            memory.setMemoryType(memoryType);
        }

        if (content != null) {
            validateRequired(content, "content");

            memory.setContent(content);
        }

        memory.setUpdatedAt(LocalDateTime.now(clock));
    }

    private static void verifyExpectedVersion(AiAutoMemory memory, long expectedVersion) {
        if (!Objects.equals(expectedVersion, memory.getVersion())) {
            throw new AiAutoMemoryConcurrentModificationException(memory.getName());
        }
    }

    private AiAutoMemory loadAndCheckOwnership(AiAutoMemoryOwner owner, long memoryId) {
        // Probe-oracle defense: the owner-scoped lookup reports "does not exist" and "exists for another owner" the
        // same way, so an authenticated attacker cannot enumerate memory ids across workspaces or environments.
        return aiMemoryRepository.findOwnedById(owner, memoryId)
            .orElseThrow(() -> new AiAutoMemoryNotFoundException("Memory not found"));
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static void validateName(String name) {
        if (name == null || !AiAutoMemory.NAME_PATTERN.matcher(name)
            .matches()) {

            throw new IllegalArgumentException(
                "name must be 1-64 characters of lowercase letters, digits, '-' or '_' — got '" + name + "'");
        }
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
