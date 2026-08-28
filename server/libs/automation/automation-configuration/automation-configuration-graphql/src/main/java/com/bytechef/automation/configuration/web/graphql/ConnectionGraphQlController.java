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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.credential.store.CredentialStoreType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * Authorization (ADMIN) is enforced on {@link WorkspaceConnectionFacade}, not here.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ConnectionGraphQlController {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionGraphQlController(WorkspaceConnectionFacade workspaceConnectionFacade) {
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @MutationMapping(name = "disconnectConnection")
    public Boolean disconnectConnection(@Argument long connectionId) {
        workspaceConnectionFacade.disconnectConnection(connectionId);

        return true;
    }

    @MutationMapping(name = "registerExistingConnection")
    public Long registerExistingConnection(@Argument RegisterExistingConnectionInput input) {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName(input.componentName())
            .connectionVersion(input.connectionVersion())
            .environmentId((int) input.environmentId())
            .name(input.name())
            .build();

        return workspaceConnectionFacade.registerExisting(
            input.workspaceId(), connectionDTO, input.credentialStoreType(), input.credentialRef());
    }

    @MutationMapping(name = "updateConnectionCredentials")
    public Boolean updateConnectionCredentials(@Argument UpdateConnectionCredentialsInput input) {
        workspaceConnectionFacade.updateConnectionCredentials(
            input.connectionId(), input.parameters(), input.version());

        return true;
    }

    @SuppressFBWarnings("EI")
    public record UpdateConnectionCredentialsInput(long connectionId, Map<String, Object> parameters, int version) {
    }

    public record RegisterExistingConnectionInput(
        String componentName,
        int connectionVersion,
        String credentialRef,
        CredentialStoreType credentialStoreType,
        long environmentId,
        String name,
        long workspaceId) {
    }
}
