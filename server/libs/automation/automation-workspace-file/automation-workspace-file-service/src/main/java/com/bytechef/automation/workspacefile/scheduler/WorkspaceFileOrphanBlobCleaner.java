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

package com.bytechef.automation.workspacefile.scheduler;

import com.bytechef.automation.workspacefile.config.AutomationWorkspaceFileOrphanCleanupProperties;
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
    prefix = "bytechef.workspace-file.orphan-cleanup",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class WorkspaceFileOrphanBlobCleaner {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceFileOrphanBlobCleaner.class);

    private final AutomationWorkspaceFileOrphanCleanupProperties properties;

    public WorkspaceFileOrphanBlobCleaner(AutomationWorkspaceFileOrphanCleanupProperties properties) {
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 60L * 60L * 1000L)
    public void cleanup() {
        log.atDebug()
            .setMessage("workspace-files orphan cleanup started")
            .log();

        // Actual orphan-blob listing requires a list API on FileStorageService that
        // doesn't exist yet. When it does, cross-check FileEntry URLs in the
        // workspace-files directory against the workspace_file row set and delete
        // blobs with no matching row. Until then, this is a no-op scaffold so the
        // @Scheduled method exists and the config property is honored.
    }
}
