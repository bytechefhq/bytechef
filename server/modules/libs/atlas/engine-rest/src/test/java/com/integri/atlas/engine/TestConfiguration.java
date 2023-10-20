/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine;

import com.integri.atlas.engine.coordinator.CoordinatorControl;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.core.task.TaskDefinition;
import com.integri.atlas.engine.core.task.description.TaskSpecification;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@SpringBootConfiguration
@ComponentScan
public class TestConfiguration {

    @MockBean
    private CoordinatorControl coordinatorControl;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private WorkflowRepository workflowRepository;

    @Bean
    TaskDefinition task1TaskSpecification() {
        return () -> TaskSpecification.create("task1");
    }

    @Bean
    TaskDefinition task2TaskSpecification() {
        return () -> TaskSpecification.create("task2");
    }
}
