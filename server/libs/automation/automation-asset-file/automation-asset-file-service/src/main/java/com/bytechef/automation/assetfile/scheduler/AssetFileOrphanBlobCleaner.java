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

package com.bytechef.automation.assetfile.scheduler;

import com.bytechef.automation.assetfile.cleanup.AssetFileOrphanBlob;
import com.bytechef.automation.assetfile.cleanup.AssetFileOrphanBlobRepository;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(
    prefix = "bytechef.asset-file.orphan-cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@SuppressFBWarnings("EI2")
public class AssetFileOrphanBlobCleaner {

    private static final Logger log = LoggerFactory.getLogger(AssetFileOrphanBlobCleaner.class);

    private final AssetFileOrphanBlobRepository orphanBlobRepository;
    private final AssetFileFileStorage fileStorage;
    private final ObjectProvider<TenantService> tenantServiceObjectProvider;

    public AssetFileOrphanBlobCleaner(
        AssetFileOrphanBlobRepository orphanBlobRepository, AssetFileFileStorage fileStorage,
        ObjectProvider<TenantService> tenantServiceObjectProvider) {

        this.orphanBlobRepository = orphanBlobRepository;
        this.fileStorage = fileStorage;
        this.tenantServiceObjectProvider = tenantServiceObjectProvider;
    }

    @Scheduled(fixedDelay = 60L * 60L * 1000L)
    public void cleanup() {
        TenantService tenantService = tenantServiceObjectProvider.getIfAvailable();

        if (tenantService == null) {
            cleanupCurrentTenant();

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::cleanupCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("Asset-file orphan-blob sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void cleanupCurrentTenant() {
        List<AssetFileOrphanBlob> orphanBlobs = orphanBlobRepository.findAll();

        if (orphanBlobs.isEmpty()) {
            return;
        }

        int reclaimed = 0;

        for (AssetFileOrphanBlob orphanBlob : orphanBlobs) {
            if (tryDelete(orphanBlob)) {
                orphanBlobRepository.deleteById(orphanBlob.getId());

                reclaimed++;
            } else {
                orphanBlob.setAttempts(orphanBlob.getAttempts() + 1);
                orphanBlob.setLastAttemptDate(Instant.now());

                orphanBlobRepository.save(orphanBlob);
            }
        }

        if (reclaimed > 0 || orphanBlobs.size() > reclaimed) {
            log.info(
                "Asset-file orphan-blob sweep reclaimed {} of {} queued blob(s)", reclaimed, orphanBlobs.size());
        }
    }

    private boolean tryDelete(AssetFileOrphanBlob orphanBlob) {
        try {
            if (!fileStorage.fileExists(orphanBlob.getFile())) {
                return true;
            }

            fileStorage.deleteFile(orphanBlob.getFile());

            return true;
        } catch (FileStorageException | IllegalStateException exception) {
            log.warn(
                "Retry delete failed for orphaned blob {} (attempt {})",
                orphanBlob.getFile(), orphanBlob.getAttempts() + 1, exception);

            return false;
        }
    }
}
