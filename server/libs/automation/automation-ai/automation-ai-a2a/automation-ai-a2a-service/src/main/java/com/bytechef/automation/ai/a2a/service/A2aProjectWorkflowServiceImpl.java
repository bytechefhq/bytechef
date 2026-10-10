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
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
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
public class A2aProjectWorkflowServiceImpl implements A2aProjectWorkflowService {

    private final A2aProjectWorkflowRepository a2aProjectWorkflowRepository;

    public A2aProjectWorkflowServiceImpl(A2aProjectWorkflowRepository a2aProjectWorkflowRepository) {
        this.a2aProjectWorkflowRepository = a2aProjectWorkflowRepository;
    }

    @Override
    public A2aProjectWorkflow create(long a2aProjectId, long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(a2aProjectId, projectDeploymentWorkflowId);

        return a2aProjectWorkflowRepository.save(a2aProjectWorkflow);
    }

    @Override
    public void delete(long a2aProjectWorkflowId) {
        a2aProjectWorkflowRepository.deleteById(a2aProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProjectWorkflow> getA2aProjectA2aProjectWorkflows(Long a2aProjectId) {
        return a2aProjectWorkflowRepository.findAllByA2aProjectId(a2aProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProjectWorkflow> getProjectDeploymentWorkflowA2aProjectWorkflows(Long projectDeploymentWorkflowId) {
        return a2aProjectWorkflowRepository.findAllByProjectDeploymentWorkflowId(projectDeploymentWorkflowId);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProjectWorkflow updateEnabled(long id, boolean enabled) {
        A2aProjectWorkflow existingA2aProjectWorkflow = a2aProjectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("A2aProjectWorkflow not found with id: " + id));

        existingA2aProjectWorkflow.setEnabled(enabled);

        return a2aProjectWorkflowRepository.save(existingA2aProjectWorkflow);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProjectWorkflow updateSkill(long id, @Nullable String skillName, @Nullable String skillDescription) {

        A2aProjectWorkflow existingA2aProjectWorkflow = a2aProjectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("A2aProjectWorkflow not found with id: " + id));

        if (skillName != null) {
            existingA2aProjectWorkflow.setSkillName(skillName);
        }

        if (skillDescription != null) {
            existingA2aProjectWorkflow.setSkillDescription(skillDescription);
        }

        return a2aProjectWorkflowRepository.save(existingA2aProjectWorkflow);
    }
}
