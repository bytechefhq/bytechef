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

package com.integri.atlas.workflow;

import com.integri.atlas.engine.coordinator.Coordinator;
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
    private Coordinator coordinator;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private WorkflowRepository workflowRepository;

    @Bean
    TaskDefinition task1Definition() {
        return () -> TaskSpecification.task("task1");
    }

    @Bean
    TaskDefinition task2Definition() {
        return () -> TaskSpecification.task("task2");
    }
}
