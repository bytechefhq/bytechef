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

package com.bytechef.automation.assetfile.service;

import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.domain.AssetFileVersion;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.repository.AssetFileVersionRepository;
import com.bytechef.automation.assetfile.util.AssetFileNameSanitizer;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI2")
public class AssetFileSystemFacadeImpl implements AssetFileSystemFacade {

    private final AssetFileService service;
    private final AssetFileFileStorage fileStorage;
    private final AssetFileMetrics metrics;
    private final AssetFileVersionRepository versionRepository;
    private final AssetFileWriteSupport writeSupport;
    private final AutomationAssetFileQuotaProperties quota;
    private final AutomationAssetFileSharingProperties sharingProperties;
    private final Tika tika;

    @SuppressFBWarnings("EI")
    public AssetFileSystemFacadeImpl(
        AssetFileService service,
        AssetFileFileStorage fileStorage,
        AssetFileMetrics metrics,
        AssetFileVersionRepository versionRepository,
        AssetFileWriteSupport writeSupport,
        AutomationAssetFileQuotaProperties quota,
        AutomationAssetFileSharingProperties sharingProperties,
        Tika tika) {

        this.service = service;
        this.fileStorage = fileStorage;
        this.metrics = metrics;
        this.versionRepository = versionRepository;
        this.writeSupport = writeSupport;
        this.quota = quota;
        this.sharingProperties = sharingProperties;
        this.tika = tika;
    }

    @Override
    public AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data) {

        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId is required");
        }

        String sanitized = writeSupport.resolveUniqueName(
            workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));
        byte[] bytes = readAllBoundedByPerFileQuota(data);

        writeSupport.enforceWorkspaceQuota(workspaceId, environment, bytes.length);

        String sniffedMime = tika.detect(bytes, sanitized);
        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        AssetFile assetFile = new AssetFile();

        assetFile.setName(sanitized);
        assetFile.setMimeType(sniffedMime);
        assetFile.setSizeBytes(bytes.length);
        assetFile.setFile(stored);
        assetFile.setSource(AssetFileSource.USER_UPLOAD);
        assetFile.setEnvironment(Environment.values()[environment]);

        AssetFile saved;

        try {
            saved = service.create(assetFile, workspaceId);
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.USER_UPLOAD, sniffedMime);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        Long workspaceId, int environment, List<Long> tagIds) {

        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId is required");
        }

        return service.findAllByWorkspaceIdAndEnvironment(workspaceId, environment, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetFile findByIdInWorkspace(Long id, Long workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId is required");
        }

        AssetFile assetFile;

        try {
            assetFile = service.findById(id);
        } catch (IllegalArgumentException exception) {
            throw new AssetFileNotFoundException(
                "Asset file %d not found in workspace %d".formatted(id, workspaceId));
        }

        Long owningWorkspaceId = assetFile.getWorkspaceId();

        if (owningWorkspaceId == null || !owningWorkspaceId.equals(workspaceId)) {
            throw new AssetFileNotFoundException(
                "Asset file %d not found in workspace %d".formatted(id, workspaceId));
        }

        return assetFile;
    }

    @Override
    @Transactional
    public AssetFile renameInWorkspace(Long id, Long workspaceId, String newName) {
        AssetFile assetFile = findByIdInWorkspace(id, workspaceId);

        return doRename(assetFile, workspaceId, newName);
    }

    @Override
    @Transactional
    public void deleteInWorkspace(Long id, Long workspaceId) {
        AssetFile assetFile = findByIdInWorkspace(id, workspaceId);

        doDelete(assetFile);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadContentInWorkspace(Long id, Long workspaceId) {
        AssetFile assetFile = findByIdInWorkspace(id, workspaceId);

        return doDownloadContent(assetFile);
    }

    @Override
    @Transactional
    public AssetFile updateContentInWorkspace(Long id, Long workspaceId, String contentType, InputStream data) {
        AssetFile assetFile = findByIdInWorkspace(id, workspaceId);

        return doUpdateContent(assetFile, workspaceId, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssetFile> fetchByPublicLinkToken(String token) {
        if (!sharingProperties.publicLinkEnabled()) {
            return Optional.empty();
        }

        return service.fetchByPublicLinkToken(token);
    }

    private AssetFile doRename(AssetFile assetFile, Long workspaceId, String newName) {
        int environment = (int) assetFile.getEnvironmentId();

        String sanitized = AssetFileNameSanitizer.sanitize(newName);
        String uniqueName = writeSupport.resolveUniqueName(workspaceId, environment, sanitized);

        assetFile.setName(uniqueName);

        return service.update(assetFile);
    }

    private void doDelete(AssetFile assetFile) {
        Long id = assetFile.getId();
        FileEntry fileEntry = assetFile.getFile();

        List<AssetFileVersion> versions = versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(id);

        service.delete(id);

        if (fileEntry != null) {
            writeSupport.scheduleBlobDeleteAfterCommit(id, fileEntry);
        }

        for (AssetFileVersion version : versions) {
            writeSupport.scheduleBlobDeleteAfterCommit(id, version.getFile());
        }
    }

    private InputStream doDownloadContent(AssetFile assetFile) {
        return fileStorage.getInputStream(assetFile.getFile());
    }

    private AssetFile doUpdateContent(AssetFile assetFile, Long workspaceId, InputStream data) {
        byte[] bytes = readAllBoundedByPerFileQuota(data);

        return updateContentInternal(assetFile, workspaceId, tika.detect(bytes, assetFile.getName()), bytes);
    }

    private AssetFile updateContentInternal(AssetFile assetFile, Long workspaceId, String mimeType, byte[] bytes) {
        int environment = (int) assetFile.getEnvironmentId();

        long delta = bytes.length - assetFile.getSizeBytes();

        if (delta > 0) {
            writeSupport.enforceWorkspaceQuota(workspaceId, environment, delta);
        }

        FileEntry previousFile = assetFile.getFile();
        String previousMimeType = assetFile.getMimeType();
        long previousSizeBytes = assetFile.getSizeBytes();

        FileEntry stored = fileStorage.storeFile(assetFile.getName(), new ByteArrayInputStream(bytes));

        assetFile.setFile(stored);
        assetFile.setMimeType(mimeType);
        assetFile.setSizeBytes(bytes.length);

        AssetFile saved;

        try {
            saved = service.update(assetFile);

            if (previousFile != null) {
                snapshotVersion(saved.getId(), previousFile, previousMimeType, previousSizeBytes);
                pruneVersions(saved.getId());
            }
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        return saved;
    }

    private void snapshotVersion(Long assetFileId, FileEntry file, String mimeType, long sizeBytes) {
        int nextVersionNumber = versionRepository.findFirstByAssetFileIdOrderByVersionNumberDesc(assetFileId)
            .map(latest -> latest.getVersionNumber() + 1)
            .orElse(1);

        AssetFileVersion version = new AssetFileVersion();

        version.setAssetFileId(assetFileId);
        version.setVersionNumber(nextVersionNumber);
        version.setFile(file);
        version.setMimeType(mimeType);
        version.setSizeBytes(sizeBytes);

        versionRepository.save(version);
    }

    private void pruneVersions(Long assetFileId) {
        int maxVersions = quota.maxVersionsPerFile();

        if (maxVersions < 0) {
            return;
        }

        List<AssetFileVersion> versions = versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(assetFileId);

        if (versions.size() <= maxVersions) {
            return;
        }

        for (AssetFileVersion excess : versions.subList(maxVersions, versions.size())) {
            versionRepository.deleteById(excess.getId());

            writeSupport.scheduleBlobDeleteAfterCommit(assetFileId, excess.getFile());
        }
    }

    private byte[] readAllBoundedByPerFileQuota(InputStream data) {
        long limit = quota.maxFileSizeBytes();

        try (InputStream inputStream = data; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            long total = 0;
            int read;

            while ((read = inputStream.read(chunk)) >= 0) {
                total += read;

                if (limit >= 0 && total > limit) {
                    throw new AssetFileQuotaExceededException(
                        "File size %d exceeds per-file limit %d".formatted(total, limit), total, limit);
                }

                buffer.write(chunk, 0, read);
            }

            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
