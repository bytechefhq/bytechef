/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.coordinator.util.TestConfigurator;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.repository.resource.workflow.ResourceBasedWorkflowRepository;
import com.bytechef.atlas.repository.task.execution.TaskExecutionRepository;
import com.bytechef.atlas.repository.workflow.mapper.JsonWorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.YamlWorkflowMapper;
import com.bytechef.atlas.service.job.JobService;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SpringBootTest
public class CoordinatorIntTest {

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TestConfigurator testConfigurator;

    @Test
    public void testStartJob_JSON() {
        Job completedJob = testConfigurator.startJob("samples/hello.json", new JsonWorkflowMapper());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testStartJob_YAML() {
        Job completedJob = testConfigurator.startJob("samples/hello.yaml", new YamlWorkflowMapper());

        Assertions.assertEquals(JobStatus.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testRequiredParams() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CoordinatorImpl coordinator = new CoordinatorImpl();

            coordinator.setJobService(new JobService(
                    null, taskExecutionRepository, new ResourceBasedWorkflowRepository(new JsonWorkflowMapper())));

            coordinator.create(MapObject.of(Collections.singletonMap("workflowId", "samples/hello.json")));
        });
    }
}
