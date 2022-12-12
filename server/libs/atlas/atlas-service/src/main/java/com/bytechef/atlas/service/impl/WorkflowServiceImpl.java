
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

package com.bytechef.atlas.service.impl;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowRepository;
import com.bytechef.atlas.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;

    @SuppressFBWarnings("EI2")
    public WorkflowServiceImpl(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public Workflow add(Workflow workflow) {
        workflow.setId(null);

        return workflowRepository.save(workflow);
    }

    @Override
    public void delete(String id) {
        workflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Workflow getWorkflow(String id) {
        return workflowRepository.findById(id)
            .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workflow> getWorkflows() {
        return StreamSupport.stream(workflowRepository.findAll()
            .spliterator(), false)
            .toList();
    }

    @Override
    public Workflow update(Workflow workflow) {
        return workflowRepository.save(workflow);
    }
}
