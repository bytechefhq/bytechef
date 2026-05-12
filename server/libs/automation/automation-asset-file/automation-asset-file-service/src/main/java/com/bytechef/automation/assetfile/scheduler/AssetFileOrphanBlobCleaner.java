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

import com.bytechef.automation.assetfile.config.AutomationAssetFileOrphanCleanupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AssetFileOrphanBlobCleaner {

    private static final Logger log = LoggerFactory.getLogger(AssetFileOrphanBlobCleaner.class);

    private final AutomationAssetFileOrphanCleanupProperties properties;

    public AssetFileOrphanBlobCleaner(AutomationAssetFileOrphanCleanupProperties properties) {
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 60L * 60L * 1000L)
    public void cleanup() {
        log.atDebug()
            .setMessage("asset-files orphan cleanup started")
            .log();

        // Actual orphan-blob listing requires a list API on FileStorageService that
        // doesn't exist yet. When it does, cross-check FileEntry URLs in the
        // asset-files directory against the asset_file row set and delete
        // blobs with no matching row. Until then, this is a no-op scaffold so the
        // @Scheduled method exists and the config property is honored.
    }
}
