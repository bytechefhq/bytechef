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
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Boolean disconnectConnection(@Argument long connectionId) {
        workspaceConnectionFacade.disconnectConnection(connectionId);

        return true;
    }
}
