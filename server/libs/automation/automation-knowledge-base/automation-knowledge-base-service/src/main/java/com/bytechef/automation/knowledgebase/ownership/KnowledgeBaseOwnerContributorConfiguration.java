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

package com.bytechef.automation.knowledgebase.ownership;

import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class KnowledgeBaseOwnerContributorConfiguration {

    public static final String KNOWLEDGE_BASE_PROJECT_NAME_PREFIX = "__KNOWLEDGE_BASE__";

    private static final String KNOWLEDGE_BASE_PROJECT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM project knowledge_base_project WHERE knowledge_base_project.id = %1$s AND " +
            "LEFT(knowledge_base_project.name, " + KNOWLEDGE_BASE_PROJECT_NAME_PREFIX.length() + ") = '" +
            KNOWLEDGE_BASE_PROJECT_NAME_PREFIX + "')";

    @Bean
    ProjectOwnerContributor knowledgeBaseProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.KNOWLEDGE_BASE, KNOWLEDGE_BASE_PROJECT_CLAIM_CONDITION);
    }
}
