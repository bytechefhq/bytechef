
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

package com.bytechef.atlas.service;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.repository.WorkflowCrudRepository;
import com.bytechef.atlas.config.WorkflowIntTestConfiguration;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.annotation.EmbeddedSql;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = WorkflowIntTestConfiguration.class,
    properties = {
        "bytechef.context-repository.provider=jdbc",
        "bytechef.persistence.provider=jdbc",
        "bytechef.workflow-repository.jdbc.enabled=true"
    })
public class WorkflowServiceIntTest {

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testCreate() {
        Workflow workflow = getWorkflow();

        workflow = workflowService.create(workflow.getDefinition(), workflow.getFormat(), Workflow.SourceType.JDBC);

        Assertions.assertEquals(workflow, OptionalUtils.get(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testDelete() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        workflowService.delete(workflow.getId());

        Assertions.assertFalse(OptionalUtils.isPresent(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testGetWorkflow() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Assertions.assertEquals(workflow, workflowService.getWorkflow(workflow.getId()));
    }

    @Test
    public void testGetWorkflows() {
        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        workflowCrudRepository.save(getWorkflow());

        Assertions.assertEquals(1, CollectionUtils.size(workflowService.getWorkflows()));
    }

    @Test
    public void testUpdate() {
        String definition = "{\"label\": \"Label\",\"tasks\": []}";
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Workflow updatedWorkflow = workflowService.update(workflow.getId(), definition);

        Assertions.assertEquals(definition, updatedWorkflow.getDefinition());
    }

    private static Workflow getWorkflow() {
        Workflow workflow = new Workflow("{\"tasks\": []}", Workflow.Format.JSON);

        workflow.setNew(true);

        return workflow;
    }
}
