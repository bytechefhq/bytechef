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

package com.bytechef.atlas.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface WorkflowService {

    Workflow create(String definition, Format format, SourceType sourceType);

    void delete(String id);

    void delete(List<String> ids);

    Workflow duplicateWorkflow(String id);

    Optional<Workflow> fetchWorkflow(String id);

    List<Workflow> getWorkflows();

    Workflow getWorkflow(String id);

    List<Workflow> getWorkflows(List<String> workflowIds);

    void refreshCache(String id);

    Workflow update(String id, String definition, int version);
}
