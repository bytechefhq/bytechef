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

package com.bytechef.automation.ai.a2a.service;

import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface A2aProjectWorkflowService {

    A2aProjectWorkflow create(long a2aProjectId, long projectDeploymentWorkflowId);

    void delete(long a2aProjectWorkflowId);

    List<A2aProjectWorkflow> getA2aProjectA2aProjectWorkflows(Long a2aProjectId);

    List<A2aProjectWorkflow> getProjectDeploymentWorkflowA2aProjectWorkflows(Long projectDeploymentWorkflowId);

    A2aProjectWorkflow updateSkill(long id, @Nullable String skillName, @Nullable String skillDescription);
}
