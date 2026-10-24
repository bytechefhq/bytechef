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

package com.bytechef.automation.datasync.event;

import com.bytechef.automation.configuration.listener.ProjectPublishPreListener;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.automation.datasync.service.DataSyncService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Validates every Data Sync of a project and brings its generated workflow up to date before the project version is
 * published. A project without Data Syncs is skipped, so its publish is never subject to the Data Sync facade's
 * {@code DATA_SYNC_PUBLISH} gate.
 *
 * @author Ivica Cardic
 */
@Component
public class DataSyncProjectPublishPreListener implements ProjectPublishPreListener {

    private final DataSyncFacade dataSyncFacade;
    private final DataSyncService dataSyncService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataSyncProjectPublishPreListener(@Lazy DataSyncFacade dataSyncFacade, DataSyncService dataSyncService) {
        this.dataSyncFacade = dataSyncFacade;
        this.dataSyncService = dataSyncService;
    }

    @Override
    public void onBeforePublishProject(long projectId) {
        if (dataSyncService.getProjectDataSyncs(projectId)
            .isEmpty()) {

            return;
        }

        dataSyncFacade.prepareProjectPublish(projectId);
    }
}
