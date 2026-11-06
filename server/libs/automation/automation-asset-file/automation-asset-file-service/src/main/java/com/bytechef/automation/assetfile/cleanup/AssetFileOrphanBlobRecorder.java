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
 * @author Ivica Cardic
 */
@Component
public class AssetFileOrphanBlobRecorder {

    private static final Logger log = LoggerFactory.getLogger(AssetFileOrphanBlobRecorder.class);

    private final AssetFileOrphanBlobRepository assetFileOrphanBlobRepository;

    public AssetFileOrphanBlobRecorder(AssetFileOrphanBlobRepository assetFileOrphanBlobRepository) {
        this.assetFileOrphanBlobRepository = assetFileOrphanBlobRepository;
    }

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
