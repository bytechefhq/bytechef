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

package com.bytechef.component.bitbucket;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.bitbucket.action.BitbucketCreateProjectAction;
import com.bytechef.component.bitbucket.action.BitbucketCreateRepositoryAction;
import com.bytechef.component.bitbucket.connection.BitbucketConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractBitbucketComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("bitbucket")
            .title("Bitbucket")
            .description(
                "Elevate your software delivery from planning to production and beyond, with built-in AI, CI/CD, and a best-in-class Jira integration."))
                    .actions(modifyActions(BitbucketCreateRepositoryAction.ACTION_DEFINITION,
                        BitbucketCreateProjectAction.ACTION_DEFINITION))
                    .connection(modifyConnection(BitbucketConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(BitbucketCreateRepositoryAction.ACTION_DEFINITION),
                        tool(BitbucketCreateProjectAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
