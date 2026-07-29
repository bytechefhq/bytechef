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
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * File-storage backed {@link AiAutoMemoryRepository}. Backs the {@code FILESYSTEM} and {@code AWS} auto-memory
 * providers; which one is in play depends solely on the {@link FileStorageService} handed to it.
 *
 * <p>
 * Memories are stored one JSON object per memory under a directory that encodes the query tuple, so the workspace
 * queries list exactly the rows they want:
 * </p>
 *
 * <pre>
 * ai_auto_memory/{workspaceId}/{principalType}_{principalId}/{environment}/{id}.json
 * </pre>
 *
 * <p>
 * A memory with no workspace uses the literal segment {@value #NO_WORKSPACE_DIRECTORY} in place of the id. It has to be
 * a segment of its own rather than an omitted one, otherwise the path would shift by a level and the workspace queries
 * would read the wrong directory; and it has to be non-numeric so it cannot collide with workspace {@code 0}, which is
 * a legitimate workspace id. Nothing lists that directory today — the repository interface exposes only
 * workspace-scoped queries, which take a primitive {@code long} and therefore never ask for it — but the rows stay
 * addressable and separable for whatever does.
 * </p>
 *
 * <p>
 * Path segments deliberately use only lowercase alphanumerics and {@code _}: the filesystem backend normalizes
 * directories to that alphabet, so a hyphen would be stripped and two distinct names could collide. Tenant isolation is
 * applied by the {@code FileStorageService} itself, so it is not repeated here.
 * </p>
 *
 * <p>
 * Semantics are best-effort, single-writer: a memory is written as a whole object and concurrent edits of the same
 * memory are last-write-wins. There is no cross-object transaction. Ordering comes from the stored {@code updatedAt},
 * because {@link FileEntry} carries no modification time.
 * </p>
 *
 * @author Ivica Cardic
 */
public class FileStorageAiAutoMemoryRepository implements AiAutoMemoryRepository {

    static final String ROOT_DIRECTORY = "ai_auto_memory";

    /**
     * Directory segment standing in for the workspace id of a memory that has none. Non-numeric on purpose so it cannot
     * be confused with workspace {@code 0}.
     */
    static final String NO_WORKSPACE_DIRECTORY = "none";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public FileStorageAiAutoMemoryRepository(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public AiAutoMemory save(AiAutoMemory memory) {
        // Timestamps are deliberately NOT assigned here: AiAutoMemoryServiceImpl owns createdAt/updatedAt (from an
        // injected Clock), exactly as it does for the JDBC binding. Assigning them here would overwrite the
        // service's values and make this backend behave differently from JDBC.
        if (memory.getId() == null) {
            memory.setId(generateId());
        }

        AiAutoMemoryDocument document = AiAutoMemoryDocument.fromDomain(memory);

        fileStorageService.storeFileContent(
            buildDirectory(
                document.workspaceId(), document.principalType(), document.principalId(), document.environment()),
            document.id() + ".json", JsonUtils.write(document), false);

        return memory;
    }

    @Override
    public void delete(AiAutoMemory memory) {
        Long id = memory.getId();

        if (id != null) {
            deleteById(id);
        }
    }

    @Override
    public void deleteById(long id) {
        for (DocumentEntry documentEntry : listAll()) {
            if (documentEntry.document()
                .id() == id) {

                fileStorageService.deleteFile(documentEntry.directory(), documentEntry.fileEntry());
            }
        }
    }

    @Override
    public void deleteAll() {
        for (DocumentEntry documentEntry : listAll()) {
            fileStorageService.deleteFile(documentEntry.directory(), documentEntry.fileEntry());
        }
    }

    @Override
    public Optional<AiAutoMemory> findById(long id) {
        return listAll().stream()
            .map(DocumentEntry::document)
            .filter(document -> document.id() == id)
            .findFirst()
            .map(AiAutoMemoryDocument::toDomain);
    }

    @Override
    public List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment) {

        return query(workspaceId, principalType, principalId, environment, null, null);
    }

    @Override
    public List<AiAutoMemory>
        findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
            long workspaceId, int principalType, long principalId, int environment, int memoryType) {

        return query(workspaceId, principalType, principalId, environment, memoryType, null);
    }

    @Override
    public List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
        long workspaceId, int principalType, long principalId, int environment, String name) {

        return query(workspaceId, principalType, principalId, environment, null, name);
    }

    private List<AiAutoMemory> query(
        long workspaceId, int principalType, long principalId, int environment, @Nullable Integer memoryType,
        @Nullable String name) {

        String directory = buildDirectory(workspaceId, principalType, principalId, environment);

        return readDocuments(directory).stream()
            .filter(document -> memoryType == null || document.memoryType() == memoryType)
            .filter(document -> name == null || Objects.equals(document.name(), name))
            .sorted(Comparator.comparing(AiAutoMemoryDocument::updatedAt, Comparator.nullsLast(
                Comparator.reverseOrder())))
            .map(AiAutoMemoryDocument::toDomain)
            .toList();
    }

    private List<AiAutoMemoryDocument> readDocuments(String directory) {
        List<AiAutoMemoryDocument> documents = new ArrayList<>();

        for (FileEntry fileEntry : fileStorageService.getFileEntries(directory)) {
            documents.add(readDocument(directory, fileEntry));
        }

        return documents;
    }

    private List<DocumentEntry> listAll() {
        List<DocumentEntry> documentEntries = new ArrayList<>();

        for (FileEntry fileEntry : fileStorageService.getFileEntries(ROOT_DIRECTORY)) {
            AiAutoMemoryDocument document = readDocument(ROOT_DIRECTORY, fileEntry);

            documentEntries.add(
                new DocumentEntry(
                    buildDirectory(
                        document.workspaceId(), document.principalType(), document.principalId(),
                        document.environment()),
                    fileEntry, document));
        }

        return documentEntries;
    }

    private AiAutoMemoryDocument readDocument(String directory, FileEntry fileEntry) {
        return JsonUtils.read(
            fileStorageService.readFileToString(directory, fileEntry), AiAutoMemoryDocument.class);
    }

    private static String buildDirectory(
        @Nullable Long workspaceId, int principalType, long principalId, int environment) {

        String workspaceDirectory = workspaceId == null ? NO_WORKSPACE_DIRECTORY : String.valueOf(workspaceId);

        return ROOT_DIRECTORY + "/" + workspaceDirectory + "/" + principalType + "_" + principalId + "/" + environment;
    }

    /**
     * Ids are generated here because file backends have no database sequence. Derived from a UUID rather than a hash so
     * collisions are not a practical concern, and kept positive to preserve the {@code long} id contract.
     */
    private static long generateId() {
        UUID uuid = UUID.randomUUID();

        return Math.abs(uuid.getMostSignificantBits()) + 1;
    }

    private record DocumentEntry(String directory, FileEntry fileEntry, AiAutoMemoryDocument document) {
    }
}
