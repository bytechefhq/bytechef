
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.project.connection;

import com.bytechef.helios.project.constant.ProjectConstants;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.connection.InstanceConnectionFetcher;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class ProjectInstanceConnectionFetcher implements InstanceConnectionFetcher {

    private final ConnectionService connectionService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceConnectionFetcher(
        ConnectionService connectionService, ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        this.connectionService = connectionService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
    }

    @Override
    public Connection getConnection(String key, String taskName) {
        ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
            projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(key, taskName);

        return connectionService.getConnection(projectInstanceWorkflowConnection.getConnectionId());
    }

    @Override
    public String getType() {
        return ProjectConstants.PROJECT;
    }
}
