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

package com.bytechef.runtime.job.configuration.workflow.contributor;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class WorkflowReservedWordContributorImpl implements WorkflowReservedWordContributor {

    private static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    private static final String CATEGORY = "category";
    private static final String CLUSTER_ELEMENTS = "clusterElements";
    private static final String COMPONENT_NAME = "componentName";
    private static final String COMPONENT_VERSION = "componentVersion";
    private static final String CONNECTIONS = "connections";
    private static final String TRIGGERS = "triggers";
    private static final String TAGS = "tags";

    @Override
    public List<String> getReservedWords() {
        return List.of(
            AUTHORIZATION_REQUIRED, CATEGORY, CLUSTER_ELEMENTS, COMPONENT_NAME, COMPONENT_VERSION, CONNECTIONS,
            TAGS, TRIGGERS);
    }
}
