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

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aProjectWorkflowFacadeImpl implements A2aProjectWorkflowFacade {

    private final A2aProjectWorkflowService a2aProjectWorkflowService;

    @SuppressFBWarnings("EI")
    public A2aProjectWorkflowFacadeImpl(A2aProjectWorkflowService a2aProjectWorkflowService) {
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#a2aProjectId, 'A2aProject', 'A2A_VIEW')")
    public List<A2aProjectWorkflow> getA2aProjectA2aProjectWorkflows(long a2aProjectId) {
        return a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(a2aProjectId);
    }

    @Override
    @PreAuthorize("hasPermission(#a2aProjectWorkflowId, 'A2aProjectWorkflow', 'A2A_EDIT')")
    public A2aProjectWorkflow updateA2aProjectWorkflowEnabled(long a2aProjectWorkflowId, boolean enabled) {
        return a2aProjectWorkflowService.updateEnabled(a2aProjectWorkflowId, enabled);
    }

    @Override
    @PreAuthorize("hasPermission(#a2aProjectWorkflowId, 'A2aProjectWorkflow', 'A2A_EDIT')")
    public A2aProjectWorkflow updateA2aProjectWorkflowSkill(
        long a2aProjectWorkflowId, @Nullable String skillName, @Nullable String skillDescription) {

        return a2aProjectWorkflowService.updateSkill(a2aProjectWorkflowId, skillName, skillDescription);
    }
}
