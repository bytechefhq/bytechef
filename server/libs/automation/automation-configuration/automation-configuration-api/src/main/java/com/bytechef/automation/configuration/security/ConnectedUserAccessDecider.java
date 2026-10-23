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

package com.bytechef.automation.configuration.security;

import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;

/**
 * Decides a connected user's access to a resource by ownership of their own connected-user project.
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserAccessDecider {

    enum Decision {
        NOT_GOVERNED, GRANT, DENY
    }

    Decision decide(Serializable id, String resourceType, String scope);

    /**
     * Like {@link #decide}, but denies a deployment scope whose target environment is not the principal's environment.
     */
    Decision decideInEnvironment(Serializable id, String resourceType, String scope, Environment environment);

    Decision decideWorkflow(String workflowId, String scope);

    Decision decideWorkspace(long workspaceId, String scope);
}
