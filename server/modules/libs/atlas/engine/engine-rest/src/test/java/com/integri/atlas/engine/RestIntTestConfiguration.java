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

package com.integri.atlas.engine;

import com.integri.atlas.engine.context.service.ContextService;
import com.integri.atlas.engine.coordinator.Coordinator;
import com.integri.atlas.engine.counter.service.CounterService;
import com.integri.atlas.engine.job.service.JobService;
import com.integri.atlas.engine.task.execution.service.TaskExecutionService;
import com.integri.atlas.engine.workflow.service.WorkflowService;
import com.integri.atlas.task.definition.TaskDefinitionHandler;
import com.integri.atlas.task.definition.dsl.DSL;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@SpringBootConfiguration
@ComponentScan
public class RestIntTestConfiguration {

    @MockBean
    private ContextService contextService;

    @MockBean
    private CounterService counterService;

    @MockBean
    private Coordinator coordinator;

    @MockBean
    private JobService jobService;

    @MockBean
    private TaskExecutionService taskExecutionService;

    @MockBean
    private WorkflowService workflowService;

    @Bean
    TaskDefinitionHandler task1TaskDefinitionHandler() {
        return () -> DSL.create("task1");
    }

    @Bean
    TaskDefinitionHandler task2TaskDefinitionHandler() {
        return () -> DSL.create("task2");
    }
}
