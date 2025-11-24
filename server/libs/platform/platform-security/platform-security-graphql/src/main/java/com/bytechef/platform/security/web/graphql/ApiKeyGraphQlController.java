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

package com.bytechef.platform.security.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for Platform Api Keys (non-workspace specific).
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ApiKeyGraphQlController {

    private final ApiKeyFacade apiKeyFacade;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ApiKeyGraphQlController(ApiKeyFacade apiKeyFacade, EnvironmentService environmentService) {
        this.apiKeyFacade = apiKeyFacade;
        this.environmentService = environmentService;
    }

    @QueryMapping(name = "adminApiKeys")
    public List<ApiKey> adminApiKeys(@Argument Long environmentId) {
        return apiKeyFacade.getAdminApiKeys(environmentId);
    }

    @QueryMapping(name = "apiKeys")
    public List<ApiKey> apiKeys(@Argument Long environmentId, @Argument ModeType type) {
        return apiKeyFacade.getApiKeys(environmentId, type);
    }

    @QueryMapping(name = "apiKey")
    public ApiKey apiKey(@Argument long id) {
        return apiKeyFacade.getApiKey(id);
    }

    @MutationMapping(name = "createApiKey")
    public String createApiKey(@Argument String name, @Argument long environmentId, @Argument ModeType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(environmentService.getEnvironment(environmentId));
        apiKey.setName(name);

        apiKey = apiKeyFacade.create(apiKey, type);

        return apiKey.getSecretKey();
    }

    @MutationMapping(name = "updateApiKey")
    public Boolean updateApiKey(@Argument long id, @Argument String name) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(id);
        apiKey.setName(name);

        apiKeyFacade.update(apiKey);

        return true;
    }

    @MutationMapping(name = "deleteApiKey")
    public Boolean deleteApiKey(@Argument long id) {
        apiKeyFacade.delete(id);

        return true;
    }

    @SchemaMapping(typeName = "ApiKey", field = "secretKey")
    public String secretKey(ApiKey apiKey) {
        return ObfuscateUtils.obfuscate(apiKey.getSecretKey(), 26, 6);
    }
}
