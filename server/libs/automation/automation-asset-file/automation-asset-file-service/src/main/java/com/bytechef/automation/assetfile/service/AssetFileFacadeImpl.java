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
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.util.AssetFileNameSanitizer;
import com.bytechef.exception.QuotaLimitExceededException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI2")
public class AssetFileFacadeImpl implements AssetFileFacade {

    private static final Logger log = LoggerFactory.getLogger(AssetFileFacadeImpl.class);

    private final AssetFileService service;
    private final AssetFileFileStorage fileStorage;
    private final AssetFileMetrics metrics;
    private final ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider;
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final AutomationAssetFileQuotaProperties quota;
    private final Tika tika;

    public AssetFileFacadeImpl(
        AssetFileService service,
        AssetFileFileStorage fileStorage,
        AssetFileMetrics metrics,
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        AutomationAssetFileQuotaProperties quota,
        Tika tika) {

        this.service = service;
        this.fileStorage = fileStorage;
        this.metrics = metrics;
        this.planLimitRejectionCounterObjectProvider = planLimitRejectionCounterObjectProvider;
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
        this.quota = quota;
        this.tika = tika;
    }

    @Override
    public AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data) {

        String sanitized = resolveUniqueName(workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));
        byte[] bytes = readAllBoundedByPerFileQuota(data);

        enforceWorkspaceQuota(workspaceId, environment, bytes.length);

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
            safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.USER_UPLOAD, sniffedMime);

        return saved;
    }

    @Override
    public AssetFile createFromAi(
        Long workspaceId, int environment, String filename, String contentType, String content,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        String sanitized = resolveUniqueName(workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        enforceSingleFileQuota(bytes.length);
        enforceWorkspaceQuota(workspaceId, environment, bytes.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        AssetFile assetFile = new AssetFile();

        assetFile.setName(sanitized);
        assetFile.setMimeType(contentType);
        assetFile.setSizeBytes(bytes.length);
        assetFile.setFile(stored);
        assetFile.setSource(AssetFileSource.AI_GENERATED);
        assetFile.setFormat(format);
        assetFile.setMetadataJson(metadataJson);
        assetFile.setGeneratedByAgentSource(generatedByAgentSource);
        assetFile.setGeneratedFromPrompt(generatedFromPrompt);
        assetFile.setEnvironment(Environment.values()[environment]);

        AssetFile saved;

        try {
            saved = service.create(assetFile, workspaceId);
        } catch (RuntimeException exception) {
            safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.AI_GENERATED, contentType);

        return saved;
    }

    @Override
    public AssetFile createBinaryFromAi(
        Long workspaceId, int environment, String filename, String contentType, byte[] data,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        String sanitized = resolveUniqueName(workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));

        enforceSingleFileQuota(data.length);
        enforceWorkspaceQuota(workspaceId, environment, data.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(data));

        AssetFile assetFile = new AssetFile();

        assetFile.setName(sanitized);
        assetFile.setMimeType(contentType);
        assetFile.setSizeBytes(data.length);
        assetFile.setFile(stored);
        assetFile.setSource(AssetFileSource.AI_GENERATED);
        assetFile.setFormat(format);
        assetFile.setMetadataJson(metadataJson);
        assetFile.setGeneratedByAgentSource(generatedByAgentSource);
        assetFile.setGeneratedFromPrompt(generatedFromPrompt);
        assetFile.setEnvironment(Environment.values()[environment]);

        AssetFile saved;

        try {
            saved = service.create(assetFile, workspaceId);
        } catch (RuntimeException exception) {
            safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.AI_GENERATED, contentType);

        return saved;
    }

    @Override
    public void delete(Long id) {
        AssetFile assetFile = service.findById(id);
        FileEntry fileEntry = assetFile.getFile();

        // Delete the DB row first; only after the transaction commits do we drop the blob. If we deleted the blob
        // first and the DB delete (or any later operation in the same transaction) failed, the rollback would
        // restore the row pointing at a blob that is permanently gone — every download would 500. Reversing the
        // order means a failed blob delete leaves an orphan (handled by a background GC), but the data the user
        // sees is always consistent.
        service.delete(id);

        if (fileEntry != null) {
            scheduleBlobDeleteAfterCommit(id, fileEntry);
        }
    }

    private void scheduleBlobDeleteAfterCommit(Long id, FileEntry fileEntry) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                fileStorage.deleteFile(fileEntry);
            } catch (RuntimeException exception) {
                // The DB row is already deleted but the blob is not. Without the counter increment ops would have
                // no signal that storage is leaking — the leak only becomes visible when a workspace quota rejects
                // a new upload. Tag the simple class name so a sudden spike in one failure mode is identifiable in
                // dashboards without needing the full WARN log line.
                log.warn("Failed to delete blob for workspace file {}", id, exception);
                metrics.recordBlobOrphan(exception.getClass()
                    .getSimpleName());
            }

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    fileStorage.deleteFile(fileEntry);
                } catch (RuntimeException exception) {
                    log.warn("Failed to delete blob for workspace file {}", id, exception);
                    metrics.recordBlobOrphan(exception.getClass()
                        .getSimpleName());
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadContent(Long id) {
        AssetFile assetFile = service.findById(id);

        return fileStorage.getInputStream(assetFile.getFile());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        Long workspaceId, int environment, List<Long> tagIds) {

        return service.findAllByWorkspaceIdAndEnvironment(workspaceId, environment, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetFile findById(Long id) {
        return service.findById(id);
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
    @Transactional(readOnly = true)
    public Long getOwningWorkspaceId(Long id) {
        AssetFile assetFile;

        try {
            assetFile = service.findById(id);
        } catch (IllegalArgumentException exception) {
            throw new AssetFileNotFoundException("Asset file %d not found".formatted(id));
        }

        Long workspaceId = assetFile.getWorkspaceId();

        if (workspaceId == null) {
            throw new AssetFileNotFoundException("Asset file %d not found".formatted(id));
        }

        return workspaceId;
    }

    @Override
    public AssetFile rename(Long id, String newName) {
        AssetFile assetFile = service.findById(id);
        Long workspaceId = resolveWorkspaceIdForFile(assetFile);
        int environment = (int) assetFile.getEnvironmentId();

        String sanitized = AssetFileNameSanitizer.sanitize(newName);
        String uniqueName = resolveUniqueName(workspaceId, environment, sanitized);

        assetFile.setName(uniqueName);

        return service.update(assetFile);
    }

    @Override
    public AssetFile cloneToEnvironment(
        Long id, Long workspaceId, int targetEnvironmentId, String newName) {

        Environment[] environments = Environment.values();

        if (targetEnvironmentId < 0 || targetEnvironmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid targetEnvironmentId: " + targetEnvironmentId);
        }

        // findByIdInWorkspace throws AssetFileNotFoundException for unknown id OR cross-workspace id, which is
        // exactly the auth gate this clone path needs. Same exception class flows through to the tool callback as
        // a typed not-found, mirroring the rest of the asset file API surface.
        AssetFile source = findByIdInWorkspace(id, workspaceId);

        String requestedName = newName != null && !newName.isBlank() ? newName : source.getName();
        String sanitized = resolveUniqueName(
            workspaceId, targetEnvironmentId, AssetFileNameSanitizer.sanitize(requestedName));

        // Materialise bytes via the file storage abstraction so this method works under any storage backend (JDBC,
        // S3, file system) without exposing the source FileEntry to the caller. Reading inside the same transaction
        // keeps the read consistent with the source row's view at this point in time.
        byte[] bytes;

        try (InputStream inputStream = fileStorage.getInputStream(source.getFile());
            ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] chunk = new byte[8192];
            int read;

            while ((read = inputStream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }

            bytes = buffer.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        enforceSingleFileQuota(bytes.length);
        enforceWorkspaceQuota(workspaceId, targetEnvironmentId, bytes.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        // Record the clone as USER_UPLOAD so the validate() invariants stay satisfied without dragging the source's
        // AI metadata onto the destination row. Promoting an AI-generated file means the user has reviewed and
        // is shipping it; attributing the destination as a user upload is the honest audit trail.
        AssetFile clone = new AssetFile();

        clone.setName(sanitized);
        clone.setMimeType(source.getMimeType());
        clone.setSizeBytes(bytes.length);
        clone.setFile(stored);
        clone.setSource(AssetFileSource.USER_UPLOAD);
        clone.setEnvironment(environments[targetEnvironmentId]);
        clone.setDescription(source.getDescription());

        AssetFile saved;

        try {
            saved = service.create(clone, workspaceId);
        } catch (RuntimeException exception) {
            safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.USER_UPLOAD, source.getMimeType());

        return saved;
    }

    @Override
    public AssetFile updateContent(Long id, String contentType, InputStream data) {
        AssetFile assetFile = service.findById(id);
        Long workspaceId = resolveWorkspaceIdForFile(assetFile);
        int environment = (int) assetFile.getEnvironmentId();

        byte[] bytes = readAllBoundedByPerFileQuota(data);

        long delta = bytes.length - assetFile.getSizeBytes();

        if (delta > 0) {
            enforceWorkspaceQuota(workspaceId, environment, delta);
        }

        String sniffedMime = tika.detect(bytes, assetFile.getName());
        FileEntry oldFile = assetFile.getFile();
        FileEntry stored = fileStorage.storeFile(assetFile.getName(), new ByteArrayInputStream(bytes));

        assetFile.setFile(stored);
        assetFile.setMimeType(sniffedMime);
        assetFile.setSizeBytes(bytes.length);

        AssetFile saved;

        try {
            saved = service.update(assetFile);
        } catch (RuntimeException exception) {
            safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        if (oldFile != null) {
            try {
                fileStorage.deleteFile(oldFile);
            } catch (RuntimeException exception) {
                log.warn("Failed to delete previous blob for workspace file {}", id, exception);
            }
        }

        return saved;
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
            throw new AssetFileQuotaExceededException(
                "File size %d exceeds per-file limit %d".formatted(bytes, limit), bytes, limit);
        }
    }

    private void enforceWorkspaceQuota(Long workspaceId, int environment, long additionalBytes) {
        enforcePlanStorageQuota(additionalBytes);

        long limit = quota.perWorkspaceTotalBytes();

        if (limit < 0) {
            return;
        }

        long current = service.sumSizeBytesByWorkspaceIdAndEnvironment(workspaceId, environment);

        if (current + additionalBytes > limit) {
            throw new AssetFileQuotaExceededException(
                "Workspace total %d would exceed limit %d".formatted(current + additionalBytes, limit),
                current + additionalBytes, limit);
        }
    }

    /**
     * Rejects the write when the tenant-wide asset-file total plus the incoming bytes would exceed the plan's
     * {@code maxStorageBytes}. Runs alongside the operator-configured per-workspace quota — the tenant ceiling spans
     * all workspaces and environments. A null limit (or no {@link PlanLimitsProvider} bean) means unlimited.
     */
    private void enforcePlanStorageQuota(long additionalBytes) {
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (planLimitsProvider == null) {
            return;
        }

        Long maxStorageBytes = planLimitsProvider.getPlanLimits(TenantContext.getCurrentTenantId())
            .maxStorageBytes();

        if (maxStorageBytes == null) {
            return;
        }

        long current = service.sumSizeBytes();

        if (current + additionalBytes > maxStorageBytes) {
            countQuotaRejection();

            throw new QuotaLimitExceededException(
                "Storage quota exceeded: the plan allows at most %d byte(s) of asset storage".formatted(
                    maxStorageBytes));
        }
    }

    private void countQuotaRejection() {
        PlanLimitRejectionCounter planLimitRejectionCounter = planLimitRejectionCounterObjectProvider.getIfAvailable();

        if (planLimitRejectionCounter != null) {
            planLimitRejectionCounter.increment("storage");
        }
    }

    /**
     * Reads {@code data} into memory but fails fast as soon as the running byte total exceeds the per-file quota.
     * Replaces a {@code readAllBytes} + post-check pair so an upload larger than the limit no longer allocates the full
     * payload (up to Spring's multipart cap) before being rejected — heap pressure is bounded by the quota itself, not
     * by the multipart parser.
     */
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

    private String resolveUniqueName(Long workspaceId, int environment, String candidate) {
        Optional<AssetFile> existing = service.fetchByWorkspaceIdAndEnvironmentAndName(
            workspaceId, environment, candidate);

        if (existing.isEmpty()) {
            return candidate;
        }

        int suffix = 2;

        while (true) {
            String attempt = appendSuffix(candidate, suffix);

            if (service.fetchByWorkspaceIdAndEnvironmentAndName(workspaceId, environment, attempt)
                .isEmpty()) {
                return attempt;
            }

            suffix++;
        }
    }

    private Long resolveWorkspaceIdForFile(AssetFile assetFile) {
        Long workspaceId = assetFile.getWorkspaceId();

        if (workspaceId == null) {
            throw new IllegalStateException(
                "No workspace id set on asset file %d".formatted(assetFile.getId()));
        }

        return workspaceId;
    }

    /**
     * Deletes a just-stored blob whose owning DB row failed to persist, attaching any cleanup failure as a suppressed
     * exception on the original cause. Without this, an S3/network failure inside {@code deleteFile} would replace the
     * real database exception with a misleading "blob delete failed" — operators would chase a storage red herring
     * instead of the row-level violation that actually triggered the rollback.
     */
    private void safeDeleteAfterRollback(FileEntry stored, RuntimeException originalCause) {
        try {
            fileStorage.deleteFile(stored);
        } catch (RuntimeException cleanupException) {
            log.warn(
                "Failed to clean up orphaned blob after rollback (original cause: {})",
                originalCause.toString(), cleanupException);

            originalCause.addSuppressed(cleanupException);
        }
    }
}
