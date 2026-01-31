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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceConnectionFacadeImpl implements WorkspaceConnectionFacade {

    private final ConnectionFacade connectionFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    public WorkspaceConnectionFacadeImpl(
        ConnectionFacade connectionFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService) {

        this.connectionFacade = connectionFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        workspaceConnectionService.create(connectionId, workspaceId);

        return connectionId;
    }

    @Override
    public void delete(long connectionId) {
        workspaceConnectionService.deleteWorkspaceConnection(connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    public void disconnectConnection(long connectionId) {
        projectDeploymentWorkflowService.deleteProjectDeploymentWorkflowConnection(connectionId);
        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(connectionId);
    }

    @Override
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        return connectionFacade.getConnections(
            componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);
    }
}
