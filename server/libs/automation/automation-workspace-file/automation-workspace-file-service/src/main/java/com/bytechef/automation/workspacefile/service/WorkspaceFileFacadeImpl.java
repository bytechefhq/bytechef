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

package com.bytechef.automation.workspacefile.service;

import com.bytechef.automation.workspacefile.config.AutomationWorkspaceFileQuotaProperties;
import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.domain.WorkspaceFileSource;
import com.bytechef.automation.workspacefile.domain.WorkspaceWorkspaceFile;
import com.bytechef.automation.workspacefile.exception.WorkspaceFileQuotaExceededException;
import com.bytechef.automation.workspacefile.file.storage.WorkspaceFileFileStorage;
import com.bytechef.automation.workspacefile.metric.WorkspaceFileMetrics;
import com.bytechef.automation.workspacefile.repository.WorkspaceWorkspaceFileRepository;
import com.bytechef.automation.workspacefile.util.WorkspaceFileNameSanitizer;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI2")
public class WorkspaceFileFacadeImpl implements WorkspaceFileFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceFileFacadeImpl.class);

    private final WorkspaceFileService service;
    private final WorkspaceWorkspaceFileRepository workspaceWorkspaceFileRepository;
    private final WorkspaceFileFileStorage fileStorage;
    private final WorkspaceFileMetrics metrics;
    private final AutomationWorkspaceFileQuotaProperties quota;
    private final Tika tika;

    public WorkspaceFileFacadeImpl(
        WorkspaceFileService service,
        WorkspaceWorkspaceFileRepository workspaceWorkspaceFileRepository,
        WorkspaceFileFileStorage fileStorage,
        WorkspaceFileMetrics metrics,
        AutomationWorkspaceFileQuotaProperties quota,
        Tika tika) {

        this.service = service;
        this.workspaceWorkspaceFileRepository = workspaceWorkspaceFileRepository;
        this.fileStorage = fileStorage;
        this.metrics = metrics;
        this.quota = quota;
        this.tika = tika;
    }

    @Override
    public WorkspaceFile createFromUpload(Long workspaceId, String filename, String contentType, InputStream data) {
        String sanitized = resolveUniqueName(workspaceId, WorkspaceFileNameSanitizer.sanitize(filename));
        byte[] bytes = readAll(data);

        enforceSingleFileQuota(bytes.length);
        enforceWorkspaceQuota(workspaceId, bytes.length);

        String sniffedMime = tika.detect(bytes, sanitized);
        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setName(sanitized);
        workspaceFile.setMimeType(sniffedMime);
        workspaceFile.setSizeBytes(bytes.length);
        workspaceFile.setFile(stored);
        workspaceFile.setSource(WorkspaceFileSource.USER_UPLOAD);

        WorkspaceFile saved;

        try {
            saved = service.create(workspaceFile, workspaceId);
        } catch (RuntimeException e) {
            fileStorage.deleteFile(stored);

            throw e;
        }

        metrics.recordCreate(WorkspaceFileSource.USER_UPLOAD, sniffedMime);

        return saved;
    }

    @Override
    public WorkspaceFile createFromAi(
        Long workspaceId, String filename, String contentType, String content,
        Short generatedByAgentSource, String generatedFromPrompt) {

        String sanitized = resolveUniqueName(workspaceId, WorkspaceFileNameSanitizer.sanitize(filename));
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        enforceSingleFileQuota(bytes.length);
        enforceWorkspaceQuota(workspaceId, bytes.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        WorkspaceFile workspaceFile = new WorkspaceFile();

        workspaceFile.setName(sanitized);
        workspaceFile.setMimeType(contentType);
        workspaceFile.setSizeBytes(bytes.length);
        workspaceFile.setFile(stored);
        workspaceFile.setSource(WorkspaceFileSource.AI_GENERATED);
        workspaceFile.setGeneratedByAgentSource(generatedByAgentSource);
        workspaceFile.setGeneratedFromPrompt(generatedFromPrompt);

        WorkspaceFile saved;

        try {
            saved = service.create(workspaceFile, workspaceId);
        } catch (RuntimeException e) {
            fileStorage.deleteFile(stored);

            throw e;
        }

        metrics.recordCreate(WorkspaceFileSource.AI_GENERATED, contentType);

        return saved;
    }

    @Override
    public void delete(Long id) {
        WorkspaceFile workspaceFile = service.findById(id);
        FileEntry fileEntry = workspaceFile.getFile();

        if (fileEntry != null) {
            try {
                fileStorage.deleteFile(fileEntry);
            } catch (RuntimeException e) {
                logger.warn("Failed to delete blob for workspace file {}: {}", id, e.getMessage());
            }
        }

        service.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadContent(Long id) {
        WorkspaceFile workspaceFile = service.findById(id);

        return fileStorage.getInputStream(workspaceFile.getFile());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceFile> findAllByWorkspaceId(Long workspaceId, List<Long> tagIds) {
        return service.findAllByWorkspaceId(workspaceId, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceFile findById(Long id) {
        return service.findById(id);
    }

    @Override
    public WorkspaceFile rename(Long id, String newName) {
        WorkspaceFile workspaceFile = service.findById(id);
        Long workspaceId = resolveWorkspaceIdForFile(id);

        String sanitized = WorkspaceFileNameSanitizer.sanitize(newName);
        String uniqueName = resolveUniqueName(workspaceId, sanitized);

        workspaceFile.setName(uniqueName);

        return service.update(workspaceFile);
    }

    @Override
    public WorkspaceFile updateContent(Long id, String contentType, InputStream data) {
        WorkspaceFile workspaceFile = service.findById(id);
        Long workspaceId = resolveWorkspaceIdForFile(id);

        byte[] bytes = readAll(data);

        enforceSingleFileQuota(bytes.length);

        long delta = bytes.length - workspaceFile.getSizeBytes();

        if (delta > 0) {
            enforceWorkspaceQuota(workspaceId, delta);
        }

        String sniffedMime = tika.detect(bytes, workspaceFile.getName());
        FileEntry oldFile = workspaceFile.getFile();
        FileEntry stored = fileStorage.storeFile(workspaceFile.getName(), new ByteArrayInputStream(bytes));

        workspaceFile.setFile(stored);
        workspaceFile.setMimeType(sniffedMime);
        workspaceFile.setSizeBytes(bytes.length);

        WorkspaceFile saved;

        try {
            saved = service.update(workspaceFile);
        } catch (RuntimeException e) {
            fileStorage.deleteFile(stored);

            throw e;
        }

        if (oldFile != null) {
            try {
                fileStorage.deleteFile(oldFile);
            } catch (RuntimeException e) {
                logger.warn("Failed to delete previous blob for workspace file {}: {}", id, e.getMessage());
            }
        }

        return saved;
    }

    @Override
    public WorkspaceFile updateTags(Long id, List<Long> tagIds) {
        WorkspaceFile workspaceFile = service.findById(id);

        workspaceFile.setTagIds(tagIds);

        return service.update(workspaceFile);
    }

    private String appendSuffix(String name, int suffix) {
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex <= 0) {
            return name + "-" + suffix;
        }

        return name.substring(0, dotIndex) + "-" + suffix + name.substring(dotIndex);
    }

    private void enforceSingleFileQuota(long bytes) {
        long limit = quota.maxFileSizeBytes();

        if (limit >= 0 && bytes > limit) {
            throw new WorkspaceFileQuotaExceededException(
                "File size %d exceeds per-file limit %d".formatted(bytes, limit), bytes, limit);
        }
    }

    private void enforceWorkspaceQuota(Long workspaceId, long additionalBytes) {
        long limit = quota.perWorkspaceTotalBytes();

        if (limit < 0) {
            return;
        }

        long current = service.sumSizeBytesByWorkspaceId(workspaceId);

        if (current + additionalBytes > limit) {
            throw new WorkspaceFileQuotaExceededException(
                "Workspace total %d would exceed limit %d".formatted(current + additionalBytes, limit),
                current + additionalBytes, limit);
        }
    }

    private byte[] readAll(InputStream data) {
        try (InputStream inputStream = data) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String resolveUniqueName(Long workspaceId, String candidate) {
        Optional<WorkspaceFile> existing = service.fetchByWorkspaceIdAndName(workspaceId, candidate);

        if (existing.isEmpty()) {
            return candidate;
        }

        int suffix = 2;

        while (true) {
            String attempt = appendSuffix(candidate, suffix);

            if (service.fetchByWorkspaceIdAndName(workspaceId, attempt)
                .isEmpty()) {
                return attempt;
            }

            suffix++;
        }
    }

    private Long resolveWorkspaceIdForFile(Long fileId) {
        WorkspaceWorkspaceFile link = workspaceWorkspaceFileRepository.findByWorkspaceFileId(fileId)
            .orElseThrow(() -> new IllegalStateException(
                "No workspace link found for workspace file %d".formatted(fileId)));

        return link.getWorkspaceId();
    }
}
