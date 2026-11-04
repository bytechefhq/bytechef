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

package com.bytechef.automation.configuration.event;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.listener.ProjectDeploymentDeleteEventListener;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class DataStorageProjectDeploymentDeleteEventListener implements ProjectDeploymentDeleteEventListener {

    private final ObjectProvider<DataStorage> dataStorageProvider;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public DataStorageProjectDeploymentDeleteEventListener(
        ObjectProvider<DataStorage> dataStorageProvider,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.dataStorageProvider = dataStorageProvider;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void onBeforeDeleteProjectDeployment(long projectDeploymentId) {
        List<ScopeData> scopeDataList = new ArrayList<>();

        scopeDataList.add(
            new ScopeData(DataStorageScope.PRINCIPAL, String.valueOf(projectDeploymentId), PlatformType.AUTOMATION));

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)) {

            scopeDataList.add(
                new ScopeData(
                    DataStorageScope.WORKFLOW, String.valueOf(projectDeploymentWorkflow.getId()),
                    PlatformType.AUTOMATION));
        }

        DataStorageScopeDeleter.deleteAfterCommit(dataStorageProvider.getIfAvailable(), scopeDataList);
    }
}
