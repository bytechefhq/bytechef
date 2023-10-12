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

package com.bytechef.atlas.configuration.repository.jdbc;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.jdbc.config.WorkflowConfigurationRepositoryIntTestConfiguration;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = WorkflowConfigurationRepositoryIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcWorkflowRepositoryIntTest {

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Test
    public void testFindById() {
        Workflow workflow = new Workflow("""
            label: My Label

            tasks:
              - name: stringNumber
                type: var/1.0/set
                parameters:
                  value: "1234"
            """,
            Workflow.Format.YAML, 0);

        workflow.setNew(true);

        workflow = workflowCrudRepository.save(workflow);

        Workflow resultWorkflow = OptionalUtils.get(workflowCrudRepository.findById(workflow.getId()));

        Assertions.assertEquals("My Label", resultWorkflow.getLabel());
        Assertions.assertEquals(1, CollectionUtils.size(resultWorkflow.getTasks()));
    }
}
