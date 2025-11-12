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

import com.bytechef.platform.security.domain.ApiKey;
import java.util.List;

/**
 * Facade for managing workspace API keys via relation table.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceApiKeyFacade {

    /**
     * Create a new ApiKey for the workspace and link it.
     *
     * @param workspaceId The workspace id
     * @param apiKey      The ApiKey aggregate (expects name, environment, etc.)
     * @return The generated secret key
     */
    String create(long workspaceId, ApiKey apiKey);

    /**
     * Deletes the API key with the specified ID from the workspace.
     *
     * @param apiKeyId The unique identifier of the API key to be deleted.
     */
    void delete(long apiKeyId);

    /**
     * Retrieves a list of API keys associated with a specific workspace and environment.
     *
     * @param workspaceId   the unique identifier of the workspace for which API keys are being retrieved
     * @param environmentId the unique identifier of the environment within the workspace for which API keys are being
     *                      retrieved
     * @return a list of ApiKey objects that correspond to the given workspace and environment
     */
    List<ApiKey> getApiKeys(long workspaceId, long environmentId);
}
