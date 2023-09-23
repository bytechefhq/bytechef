
/*
 * Copyright 2021 <your company/name>.
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

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface WorkflowService {

    Workflow create(String definition, Workflow.Format format, Workflow.SourceType sourceType);

    void delete(String id);

    Workflow duplicateWorkflow(String id);

    Workflow getWorkflow(String id);

    List<Workflow> getWorkflows();

    List<Workflow> getWorkflows(List<String> workflowIds);

    Workflow update(String id, String definition);
}
