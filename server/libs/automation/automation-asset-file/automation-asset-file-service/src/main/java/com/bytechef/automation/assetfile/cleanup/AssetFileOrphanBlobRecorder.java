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

package com.bytechef.automation.assetfile.cleanup;

import com.bytechef.file.storage.domain.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enqueues a blob whose delete failed for retry by {@code AssetFileOrphanBlobCleaner}.
 *
 * <p>
 * {@code REQUIRES_NEW} because two of the three call sites run in transaction states where a plain participating write
 * would be lost: the rollback-cleanup path runs inside a transaction already marked rollback-only (the queue row would
 * be rolled back along with everything else), and the after-commit path runs outside any transaction. A fresh,
 * independently committed transaction covers all call sites uniformly.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class AssetFileOrphanBlobRecorder {

    private static final Logger log = LoggerFactory.getLogger(AssetFileOrphanBlobRecorder.class);

    private final AssetFileOrphanBlobRepository assetFileOrphanBlobRepository;

    public AssetFileOrphanBlobRecorder(AssetFileOrphanBlobRepository assetFileOrphanBlobRepository) {
        this.assetFileOrphanBlobRepository = assetFileOrphanBlobRepository;
    }

    /**
     * Records the blob for later delete retry. Never throws — the recorder is called from failure-handling paths where
     * a second failure must not mask the original one; if even the queue insert fails, the leak is logged and the blob
     * is lost to the retry mechanism (the {@code bytechef_asset_file_blob_orphan_total} metric still counted it).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(FileEntry fileEntry) {
        try {
            AssetFileOrphanBlob orphanBlob = new AssetFileOrphanBlob();

            orphanBlob.setFile(fileEntry);

            assetFileOrphanBlobRepository.save(orphanBlob);
        } catch (RuntimeException exception) {
            log.warn("Failed to enqueue orphaned blob {} for cleanup retry", fileEntry.getName(), exception);
        }
    }
}
