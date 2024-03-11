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

package com.bytechef.atlas.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import java.util.List;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public interface WorkflowService {

    Workflow create(
        @NonNull String definition, @NonNull Format format, @NonNull SourceType sourceType, @NonNull int type);

    void delete(@NonNull String id);

    Workflow duplicateWorkflow(@NonNull String id);

    Workflow getWorkflow(@NonNull String id);

    List<Workflow> getWorkflows(int type);

    List<Workflow> getWorkflows(@NonNull List<String> workflowIds);

    void refreshCache(@NonNull String id);

    Workflow update(@NonNull String id, @NonNull String definition, int version);
}
