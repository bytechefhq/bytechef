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
 * File-storage backed {@link AiAutoMemoryRepository}. Backs the {@code FILESYSTEM} and {@code AWS} auto-memory
 * providers; which one is in play depends solely on the {@link FileStorageService} handed to it.
 *
 * <p>
 * Memories are stored one JSON object per memory, named after its id, under a directory that encodes its owner:
 * </p>
 *
 * <pre>
 * ai_auto_memory/{workspaceId}/{principalType}_{principalId}/{environment}/{id}.json
 * </pre>
 *
 * <p>
 * Path segments deliberately use only lowercase alphanumerics and {@code _}: the filesystem backend normalizes
 * directories to that alphabet, so a hyphen would be stripped and two distinct names could collide. Tenant isolation is
 * applied by the {@code FileStorageService} itself, so it is not repeated here.
 * </p>
 *
 * <p>
 * A per-owner query lists exactly its owner's directory, and a lookup by id opens the one document directly. The
 * workspace-wide queries list the whole workspace directory and filter environment and type in memory, since
 * environment sits below the principal segment. Every read also matches the owner — or, workspace-wide, the workspace —
 * stored in the document itself, so a listing that strays outside its directory cannot return foreign memories.
 * </p>
 *
 * <p>
 * A memory is written as a whole object. Saves and deletes compare the stored {@link AiAutoMemory#getVersion() version}
 * first, which rejects a stale copy, though — with no compare-and-swap in the storage API — two writers racing inside
 * that same check can still both land. There is no cross-object transaction and no unique-name enforcement. Ordering
 * comes from the stored {@code updatedAt}, because {@link FileEntry} carries no modification time. A document that
 * cannot be parsed is logged and skipped, so one corrupt file cannot take down every lookup that lists its directory; a
 * storage failure while reading is never mistaken for a missing memory and propagates.
 * </p>
 *
 * @author Ivica Cardic
 */
public class FileStorageAiAutoMemoryRepository implements AiAutoMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(FileStorageAiAutoMemoryRepository.class);

    static final String ROOT_DIRECTORY = "ai_auto_memory";

    /**
     * The largest integer a JavaScript {@code number} represents exactly (2^53 - 1). Ids travel to the browser as
     * GraphQL {@code ID} strings and are parsed into numbers there, so a larger id would be rounded and no longer
     * address the memory it came from.
     */
    static final long MAX_SAFE_ID = (1L << 53) - 1;

    private static final Comparator<AiAutoMemory> NEWEST_FIRST = Comparator.comparing(
        AiAutoMemory::getUpdatedAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileStorageAiAutoMemoryRepository(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Timestamps are owned by the service. Returns the stored memory with its assigned id and version; the instance
     * passed in is left as it was.
     */
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

    /**
     * Fails — like the JDBC binding's version check — when the document is gone or holds a newer version, so a delete
     * never reports success without deleting.
     */
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

    /**
     * The owner-agnostic counterpart of {@link #query}: reads the whole workspace directory rather than one owner's, so
     * it spans every owner.
     */
    @Override
    public List<AiAutoMemory> findByWorkspace(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType) {

        return readWorkspace(workspaceId, environment).stream()
            .filter(memory -> memoryType == null || memory.getMemoryType() == memoryType)
            .sorted(NEWEST_FIRST)
            .toList();
    }

    /**
     * There is no directory holding exactly one workspace and environment — environment sits below the principal
     * segment, so the workspace's directory is the narrowest one that holds every principal. It is listed in full and
     * the environment is matched on the stored memory. Ordered by principal type then id so this backend and the
     * relational one agree on more than set equality.
     */
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

    /**
     * The workspace's memories in one environment. Environment is filtered in memory because it sits below the
     * principal segment, so no single directory holds exactly one workspace and environment.
     */
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

    /**
     * Empty when the file does not exist — including when it is deleted between the existence check and the lookup,
     * which a storage backend that fails the lookup of a missing file reports as a {@link FileStorageException}.
     */
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

    /**
     * Only a corrupt document — one that does not deserialize, lacks a value every memory needs, or sits in a file its
     * id does not name (a copy placed by hand, which a save would write elsewhere) — is skipped; a memory that merely
     * breaks a format rule tightened after it was stored still loads. A read failure propagates unless the file is gone
     * — deleted between being listed and being read — because reporting an unreadable memory as missing would let a
     * create pass the duplicate-name check and a lookup tell the caller a memory it still holds does not exist.
     */
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

    /**
     * A document whose stored owner differs from the directory it sits in was copied there by hand; a save would never
     * put it there. It is skipped — the directory's owner does not hold it — but logged, so the memory does not just
     * vanish without a trace.
     */
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

    /**
     * Ids are generated here because file backends have no database sequence. Derived from a random UUID rather than a
     * hash and kept within {@code 1..}{@link #MAX_SAFE_ID}. The mask keeps the UUID's fixed 4-bit version field, so an
     * id carries about 49 random bits — collisions within one owner's directory remain negligible in practice.
     */
    static long generateId() {
        UUID uuid = UUID.randomUUID();

        long id = uuid.getMostSignificantBits() & MAX_SAFE_ID;

        return id == 0 ? 1 : id;
    }

    private record PrincipalKey(AiAutoMemoryPrincipalType principalType, long principalId) {
    }
}
