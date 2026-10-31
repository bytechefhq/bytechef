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

package com.bytechef.platform.configuration.workflow.diff;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionDiff;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowDefinitionDiffServiceImpl implements WorkflowDefinitionDiffService {

    @Override
    public WorkflowDefinitionDiff diff(@Nullable Workflow oldWorkflow, @Nullable Workflow newWorkflow) {
        Map<String, Object> oldNormalized = oldWorkflow == null ? null : WorkflowDefinitionNormalizer.normalize(
            oldWorkflow);
        Map<String, Object> newNormalized = newWorkflow == null ? null : WorkflowDefinitionNormalizer.normalize(
            newWorkflow);

        String oldDefinition = oldNormalized == null ? null : WorkflowDefinitionNormalizer.toJson(oldNormalized);
        String newDefinition = newNormalized == null ? null : WorkflowDefinitionNormalizer.toJson(newNormalized);

        if (oldNormalized == null) {
            return new WorkflowDefinitionDiff(WorkflowChangeType.ADDED, List.of(), null, newDefinition);
        }

        if (newNormalized == null) {
            return new WorkflowDefinitionDiff(WorkflowChangeType.REMOVED, List.of(), oldDefinition, null);
        }

        if (oldNormalized.equals(newNormalized)) {
            return new WorkflowDefinitionDiff(
                WorkflowChangeType.UNCHANGED, List.of(), oldDefinition, newDefinition);
        }

        return new WorkflowDefinitionDiff(WorkflowChangeType.MODIFIED, List.of(), oldDefinition, newDefinition);
    }
}
