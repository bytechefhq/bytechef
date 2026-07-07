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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * Lists a workspace's connections for a component, scoped to the current environment. Serves the copilot
 * {@code listConnectionsForComponent} tool for the in-editor / AI Hub surfaces, which carry a workspace context. The
 * lookup runs inside the calling user's rehydrated security context so workspace authorization is enforced.
 *
 * @author Ivica Cardic
 */
public class WorkspaceCopilotConnectionLister implements CopilotConnectionLister {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;
    private final PropertyOptionsResolver propertyOptionsResolver;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceCopilotConnectionLister(
        WorkspaceConnectionFacade workspaceConnectionFacade, PropertyOptionsResolver propertyOptionsResolver) {

        this.workspaceConnectionFacade = workspaceConnectionFacade;
        this.propertyOptionsResolver = propertyOptionsResolver;
    }

    @Override
    public boolean supports(CopilotConnectionRequest request) {
        return request.workspaceId() != null;
    }

    @Override
    public List<CopilotConnection> listConnections(CopilotConnectionRequest request) {
        List<ConnectionDTO> connectionDTOs = propertyOptionsResolver.withUserSecurityContext(
            request.userId(),
            () -> workspaceConnectionFacade.getConnections(
                Objects.requireNonNull(request.workspaceId()), request.componentName(), request.connectionVersion(),
                (long) request.environmentId(), null));

        return connectionDTOs.stream()
            .map(connectionDTO -> new CopilotConnection(
                connectionDTO.id(), connectionDTO.name(), connectionDTO.environmentId(), connectionDTO.active()))
            .toList();
    }
}
