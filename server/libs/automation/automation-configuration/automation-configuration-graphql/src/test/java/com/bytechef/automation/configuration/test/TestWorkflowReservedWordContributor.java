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

package com.bytechef.automation.configuration.test;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import java.util.List;

/**
 * Test-only stand-in for the production
 * {@code com.bytechef.platform.configuration.workflow.contributor.WorkflowReservedWordContributorImpl}. Mirrors the
 * platform-configuration-service contributor's word list so {@link com.bytechef.atlas.configuration.domain.Workflow}'s
 * validator accepts {@code triggers} / {@code clusterElements} / etc. without dragging the full
 * platform-configuration-service module into the automation-configuration-graphql test classpath.
 *
 * <p>
 * Registered via {@code META-INF/services/com.bytechef.atlas.configuration.workflow.contributor
 * .WorkflowReservedWordContributor} under {@code src/test/resources}.
 *
 * @author Ivica Cardic
 */
public class TestWorkflowReservedWordContributor implements WorkflowReservedWordContributor {

    @Override
    public List<String> getReservedWords() {
        return List.of(
            "authorizationRequired", "category", "clusterElements", "componentName", "componentVersion", "connections",
            "groupName", "internalOnly", "objectName", "tags", "triggers");
    }
}
