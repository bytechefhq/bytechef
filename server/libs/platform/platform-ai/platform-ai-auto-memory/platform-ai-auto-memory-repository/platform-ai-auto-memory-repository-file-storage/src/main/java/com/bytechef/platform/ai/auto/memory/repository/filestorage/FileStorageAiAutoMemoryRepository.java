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

package com.bytechef.platform.ai.auto.memory.repository.filestorage;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
public class FileStorageAiAutoMemoryRepository implements AiAutoMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(FileStorageAiAutoMemoryRepository.class);

    static final String ROOT_DIRECTORY = "ai_auto_memory";

    static final long MAX_SAFE_ID = (1L << 53) - 1;

    private static final Comparator<AiAutoMemory> NEWEST_FIRST = Comparator.comparing(
        AiAutoMemory::getUpdatedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileStorageAiAutoMemoryRepository(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public AiAutoMemory save(AiAutoMemory memory) {
        String directory = buildDirectory(memory);
        Long id = memory.getId();
        long version;

        if (id == null) {
            id = generateId();
            version = 0;
        } else {
            long existingId = id;

            AiAutoMemory storedMemory = readMemory(directory, existingId)
                .orElseThrow(() -> new OptimisticLockingFailureException(
                    "Memory " + existingId + " no longer exists"));

            checkVersion(memory, storedMemory);

            version = memory.getVersion() + 1;
        }

        AiAutoMemoryDocument document = AiAutoMemoryDocument.fromDomain(memory, id, version);

        AiAutoMemory savedMemory = document.toDomain();

        fileStorageService.storeFileContent(directory, toFilename(id), JsonUtils.write(document), false);

        return savedMemory;
    }

    @Override
    public void delete(AiAutoMemory memory) {
        Long id = memory.getId();

        if (id == null) {
            throw new IllegalArgumentException("A memory that was never saved cannot be deleted");
        }

        String directory = buildDirectory(memory);

        FileEntry fileEntry = findFileEntry(directory, toFilename(id))
            .orElseThrow(() -> new OptimisticLockingFailureException("Memory " + id + " no longer exists"));

        AiAutoMemory storedMemory = readMemory(directory, fileEntry)
            .orElseThrow(() -> new OptimisticLockingFailureException("Memory " + id + " no longer exists"));

        checkVersion(memory, storedMemory);

        fileStorageService.deleteFile(directory, fileEntry);
    }

    @Override
    public Optional<AiAutoMemory> findOwnedById(AiAutoMemoryOwner owner, long id) {
        String directory = buildDirectory(owner);

        return readMemory(directory, id).filter(memory -> isStoredWithOwner(memory, owner, directory));
    }

    @Override
    public List<AiAutoMemory> findByOwner(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType) {
        return query(owner, memoryType, null);
    }

    @Override
    public List<AiAutoMemory> findAllByOwnerAndName(AiAutoMemoryOwner owner, String name) {
        return query(owner, null, name);
    }

    @Override
    public List<AiAutoMemory> findByWorkspace(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType) {

        return readWorkspace(workspaceId, environment).stream()
            .filter(memory -> memoryType == null || memory.getMemoryType() == memoryType)
            .sorted(NEWEST_FIRST)
            .toList();
    }

    @Override
    public List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment) {
        Map<PrincipalKey, Long> memoryCountsByPrincipal = readWorkspace(workspaceId, environment).stream()
            .collect(
                Collectors.groupingBy(
                    memory -> new PrincipalKey(memory.getPrincipalType(), memory.getPrincipalId()),
                    Collectors.counting()));

        List<AiAutoMemoryPrincipalCount> principalCounts = new ArrayList<>();

        for (Map.Entry<PrincipalKey, Long> memoryCountEntry : memoryCountsByPrincipal.entrySet()) {
            PrincipalKey principalKey = memoryCountEntry.getKey();
            long memoryCount = memoryCountEntry.getValue();

            principalCounts.add(
                new AiAutoMemoryPrincipalCount(
                    principalKey.principalType(), principalKey.principalId(), Math.toIntExact(memoryCount)));
        }

        principalCounts.sort(
            Comparator.comparing(AiAutoMemoryPrincipalCount::principalType)
                .thenComparingLong(AiAutoMemoryPrincipalCount::principalId));

        return principalCounts;
    }

    private List<AiAutoMemory> query(
        AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType, @Nullable String name) {

        String directory = buildDirectory(owner);

        return readMemories(directory).stream()
            .filter(memory -> isStoredWithOwner(memory, owner, directory))
            .filter(memory -> memoryType == null || memory.getMemoryType() == memoryType)
            .filter(memory -> name == null || Objects.equals(memory.getName(), name))
            .sorted(NEWEST_FIRST)
            .toList();
    }

    private List<AiAutoMemory> readWorkspace(long workspaceId, Environment environment) {
        String directory = buildWorkspaceDirectory(workspaceId);

        return readMemories(directory).stream()
            .filter(memory -> isStoredWithWorkspace(memory, workspaceId, directory))
            .filter(memory -> memory.getEnvironment() == environment)
            .toList();
    }

    private List<AiAutoMemory> readMemories(String directory) {
        List<AiAutoMemory> memories = new ArrayList<>();

        for (FileEntry fileEntry : fileStorageService.getFileEntries(directory)) {
            readMemory(directory, fileEntry).ifPresent(memories::add);
        }

        return memories;
    }

    private Optional<AiAutoMemory> readMemory(String directory, long id) {
        return findFileEntry(directory, toFilename(id)).flatMap(fileEntry -> readMemory(directory, fileEntry));
    }

    private Optional<FileEntry> findFileEntry(String directory, String filename) {
        if (!fileStorageService.fileExists(directory, filename)) {
            return Optional.empty();
        }

        try {
            return Optional.of(fileStorageService.getFileEntry(directory, filename));
        } catch (FileStorageException fileStorageException) {
            if (isGone(directory, filename, fileStorageException)) {
                return Optional.empty();
            }

            throw fileStorageException;
        }
    }

    private boolean isGone(String directory, String filename, FileStorageException fileStorageException) {
        try {
            return !fileStorageService.fileExists(directory, filename);
        } catch (RuntimeException existsException) {
            fileStorageException.addSuppressed(existsException);

            throw fileStorageException;
        }
    }

    private Optional<AiAutoMemory> readMemory(String directory, FileEntry fileEntry) {
        String json;

        try {
            json = fileStorageService.readFileToString(directory, fileEntry);
        } catch (FileStorageException fileStorageException) {
            boolean fileExists;

            try {
                fileExists = fileStorageService.fileExists(directory, fileEntry);
            } catch (RuntimeException existsException) {
                fileStorageException.addSuppressed(existsException);

                throw fileStorageException;
            }

            if (!fileExists) {
                return Optional.empty();
            }

            throw fileStorageException;
        }

        try {
            AiAutoMemoryDocument document = JsonUtils.read(json, AiAutoMemoryDocument.class);

            AiAutoMemory memory = document.toDomain();

            if (!toFilename(memory.getId()).equals(fileEntry.getName())) {
                throw new IllegalStateException("Document of memory " + memory.getId() + " is stored under another id");
            }

            return Optional.of(memory);
        } catch (RuntimeException exception) {
            log.error(
                "Skipping corrupt auto-memory document {} listed under {}", fileEntry.getName(), directory,
                exception);

            return Optional.empty();
        }
    }

    private static boolean isStoredWithOwner(AiAutoMemory memory, AiAutoMemoryOwner owner, String directory) {
        AiAutoMemoryOwner storedOwner = memory.getOwner();

        if (owner.equals(storedOwner)) {
            return true;
        }

        log.warn("Skipping auto-memory document {} under {}: it belongs to {}", memory.getId(), directory, storedOwner);

        return false;
    }

    private static boolean isStoredWithWorkspace(AiAutoMemory memory, long workspaceId, String directory) {
        if (memory.getWorkspaceId() == workspaceId) {
            return true;
        }

        log.warn(
            "Skipping auto-memory document {} under {}: it belongs to workspace {}", memory.getId(), directory,
            memory.getWorkspaceId());

        return false;
    }

    private static void checkVersion(AiAutoMemory memory, AiAutoMemory storedMemory) {
        if (!Objects.equals(memory.getVersion(), storedMemory.getVersion())) {
            throw new OptimisticLockingFailureException(
                "Memory " + memory.getId() + " is at version " + storedMemory.getVersion() + ", not "
                    + memory.getVersion());
        }
    }

    private static String toFilename(long id) {
        return id + ".json";
    }

    private static String buildDirectory(AiAutoMemory memory) {
        return buildDirectory(memory.getOwner());
    }

    private static String buildDirectory(AiAutoMemoryOwner owner) {
        AiAutoMemoryPrincipalType principalType = owner.principalType();
        Environment environment = owner.environment();

        return buildWorkspaceDirectory(owner.workspaceId()) + "/" + principalType.ordinal() + "_" + owner.principalId()
            + "/" + environment.ordinal();
    }

    private static String buildWorkspaceDirectory(long workspaceId) {
        return ROOT_DIRECTORY + "/" + workspaceId;
    }

    static long generateId() {
        UUID uuid = UUID.randomUUID();

        long id = uuid.getMostSignificantBits() & MAX_SAFE_ID;

        return id == 0 ? 1 : id;
    }

    private record PrincipalKey(AiAutoMemoryPrincipalType principalType, long principalId) {
    }
}
