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

package com.bytechef.automation.configuration.callback;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Order(1)
@Component
public class ProjectWorkflowCallback implements BeforeConvertCallback<ProjectWorkflow> {

    @Override
    public ProjectWorkflow onBeforeConvert(ProjectWorkflow projectWorkflow) {
        if (projectWorkflow.getId() == null && projectWorkflow.getUuid() == null) {
            projectWorkflow.setUuid(UUID.randomUUID());
        }

        return projectWorkflow;
    }
}
