/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.automation.configuration.config;

import com.bytechef.atlas.configuration.repository.config.contributor.GitWorkflowRepositoryPropertiesContributor.GitWorkflowRepositoryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.workflow.repository.git")
public class ProjectGitWorkflowRepositoryTypeProperties {

    private GitWorkflowRepositoryProperties projects;

    public GitWorkflowRepositoryProperties getProjects() {
        return projects;
    }

    public void setProjects(GitWorkflowRepositoryProperties projects) {
        this.projects = projects;
    }
}
