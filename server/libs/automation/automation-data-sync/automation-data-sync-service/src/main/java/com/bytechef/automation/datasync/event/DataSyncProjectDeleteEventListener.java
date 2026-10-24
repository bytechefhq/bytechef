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

import com.bytechef.automation.configuration.listener.ProjectDeleteEventListener;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Removes every Data Sync of a project before the project itself is deleted.
 *
 * <p>
 * {@code @Lazy} because {@code ProjectFacadeImpl} takes its delete listeners by constructor while
 * {@code DataSyncFacadeImpl} depends on the project services, which would otherwise form a construction cycle.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class DataSyncProjectDeleteEventListener implements ProjectDeleteEventListener {

    private final DataSyncFacade dataSyncFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DataSyncProjectDeleteEventListener(@Lazy DataSyncFacade dataSyncFacade) {
        this.dataSyncFacade = dataSyncFacade;
    }

    @Override
    public void onBeforeDeleteProject(long projectId) {
        dataSyncFacade.deleteProjectDataSyncs(projectId);
    }
}
