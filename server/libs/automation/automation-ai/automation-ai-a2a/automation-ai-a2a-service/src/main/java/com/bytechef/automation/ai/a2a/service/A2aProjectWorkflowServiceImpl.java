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
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link A2aProjectWorkflowService} interface.
 *
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
    public A2aProjectWorkflow create(A2aProjectWorkflow a2aProjectWorkflow) {
        return a2aProjectWorkflowRepository.save(a2aProjectWorkflow);
    }

    @Override
    public A2aProjectWorkflow create(Long a2aProjectId, Long projectDeploymentWorkflowId) {
        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(a2aProjectId, projectDeploymentWorkflowId);

        return create(a2aProjectWorkflow);
    }

    @Override
    public void delete(long a2aProjectWorkflowId) {
        a2aProjectWorkflowRepository.deleteById(a2aProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<A2aProjectWorkflow> fetchA2aProjectWorkflow(long a2aProjectWorkflowId) {
        return a2aProjectWorkflowRepository.findById(a2aProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aProjectWorkflow> getA2aProjectWorkflows() {
        return a2aProjectWorkflowRepository.findAll();
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
    public A2aProjectWorkflow update(A2aProjectWorkflow a2aProjectWorkflow) {
        A2aProjectWorkflow currentA2aProjectWorkflow =
            a2aProjectWorkflowRepository.findById(a2aProjectWorkflow.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "A2aProjectWorkflow not found with id: " + a2aProjectWorkflow.getId()));

        currentA2aProjectWorkflow.setA2aProjectId(a2aProjectWorkflow.getA2aProjectId());
        currentA2aProjectWorkflow.setProjectDeploymentWorkflowId(a2aProjectWorkflow.getProjectDeploymentWorkflowId());
        currentA2aProjectWorkflow.setVersion(a2aProjectWorkflow.getVersion());

        return a2aProjectWorkflowRepository.save(currentA2aProjectWorkflow);
    }

    @Override
    public A2aProjectWorkflow updateParameters(long id, Map<String, ?> parameters) {
        A2aProjectWorkflow existingA2aProjectWorkflow = a2aProjectWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("A2aProjectWorkflow not found with id: " + id));

        existingA2aProjectWorkflow.setParameters(parameters);

        return a2aProjectWorkflowRepository.save(existingA2aProjectWorkflow);
    }
}
