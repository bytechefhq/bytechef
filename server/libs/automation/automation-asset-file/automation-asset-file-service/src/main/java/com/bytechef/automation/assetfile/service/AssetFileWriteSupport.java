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

import com.bytechef.automation.assetfile.cleanup.AssetFileOrphanBlobRecorder;
import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Service
@SuppressFBWarnings("EI2")
class AssetFileWriteSupport {

    private static final Logger log = LoggerFactory.getLogger(AssetFileWriteSupport.class);

    private final AssetFileService service;
    private final AssetFileFileStorage fileStorage;
    private final AssetFileMetrics metrics;
    private final AssetFileOrphanBlobRecorder orphanBlobRecorder;
    private final AutomationAssetFileQuotaProperties quota;

    @SuppressFBWarnings("EI")
    AssetFileWriteSupport(
        AssetFileService service,
        AssetFileFileStorage fileStorage,
        AssetFileMetrics metrics,
        AssetFileOrphanBlobRecorder orphanBlobRecorder,
        AutomationAssetFileQuotaProperties quota) {

        this.service = service;
        this.fileStorage = fileStorage;
        this.metrics = metrics;
        this.orphanBlobRecorder = orphanBlobRecorder;
        this.quota = quota;
    }

    String resolveUniqueName(Long workspaceId, int environment, String candidate) {
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

    void enforceWorkspaceQuota(Long workspaceId, int environment, long additionalBytes) {
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

    Long resolveWorkspaceIdForFile(AssetFile assetFile) {
        Long workspaceId = assetFile.getWorkspaceId();

        if (workspaceId == null) {
            throw new IllegalStateException(
                "No workspace id set on asset file %d".formatted(assetFile.getId()));
        }

        return workspaceId;
    }

    void safeDeleteAfterRollback(FileEntry stored, RuntimeException originalCause) {
        try {
            fileStorage.deleteFile(stored);
        } catch (RuntimeException cleanupException) {
            log.warn(
                "Failed to clean up orphaned blob after rollback (original cause: {})",
                originalCause.toString(), cleanupException);

            originalCause.addSuppressed(cleanupException);

            orphanBlobRecorder.record(stored);
        }
    }

    void scheduleBlobDeleteAfterCommit(Long id, FileEntry fileEntry) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteBlobOrEnqueueOrphan(id, fileEntry);

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                deleteBlobOrEnqueueOrphan(id, fileEntry);
            }
        });
    }

    private String appendSuffix(String name, int suffix) {
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex <= 0) {
            return name + "-" + suffix;
        }

        return name.substring(0, dotIndex) + "-" + suffix + name.substring(dotIndex);
    }

    private void deleteBlobOrEnqueueOrphan(Long id, FileEntry fileEntry) {
        try {
            fileStorage.deleteFile(fileEntry);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete blob for workspace file {}", id, exception);
            metrics.recordBlobOrphan(exception.getClass()
                .getSimpleName());
            orphanBlobRecorder.record(fileEntry);
        }
    }
}
