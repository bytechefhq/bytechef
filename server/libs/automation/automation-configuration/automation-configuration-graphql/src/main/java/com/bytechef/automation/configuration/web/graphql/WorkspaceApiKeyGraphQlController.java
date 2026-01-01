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
import com.bytechef.automation.configuration.facade.WorkspaceApiKeyFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for Workspace Api Keys.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class WorkspaceApiKeyGraphQlController {

    private final ApiKeyFacade apiKeyFacade;
    private final EnvironmentService environmentService;
    private final WorkspaceApiKeyFacade workspaceApiKeyFacade;

    @SuppressFBWarnings("EI")
    public WorkspaceApiKeyGraphQlController(
        ApiKeyFacade apiKeyFacade, EnvironmentService environmentService, WorkspaceApiKeyFacade workspaceApiKeyFacade) {

        this.apiKeyFacade = apiKeyFacade;
        this.environmentService = environmentService;
        this.workspaceApiKeyFacade = workspaceApiKeyFacade;
    }

    @QueryMapping(name = "workspaceApiKeys")
    public List<ApiKey> workspaceApiKeys(@Argument long workspaceId, @Argument long environmentId) {
        return workspaceApiKeyFacade.getApiKeys(workspaceId, environmentId);
    }

    @MutationMapping(name = "createWorkspaceApiKey")
    public String createWorkspaceApiKey(
        @Argument long workspaceId, @Argument String name, @Argument Long environmentId) {

        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(environmentService.getEnvironment(environmentId));
        apiKey.setName(name);

        return workspaceApiKeyFacade.create(workspaceId, apiKey);
    }

    @MutationMapping(name = "deleteWorkspaceApiKey")
    public Boolean deleteWorkspaceApiKey(@Argument long apiKeyId) {
        workspaceApiKeyFacade.delete(apiKeyId);

        return true;
    }

    @MutationMapping(name = "updateWorkspaceApiKey")
    public Boolean updateWorkspaceApiKey(@Argument long apiKeyId, @Argument String name) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(apiKeyId);
        apiKey.setName(name);

        apiKeyFacade.update(apiKey);

        return true;
    }
}
