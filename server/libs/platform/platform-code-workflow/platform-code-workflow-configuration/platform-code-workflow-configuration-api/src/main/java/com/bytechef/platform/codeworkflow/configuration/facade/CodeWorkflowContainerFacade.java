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

package com.bytechef.platform.codeworkflow.configuration.facade;

import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.constant.AppType;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface CodeWorkflowContainerFacade {

    CodeWorkflowContainer create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, AppType type);
}
