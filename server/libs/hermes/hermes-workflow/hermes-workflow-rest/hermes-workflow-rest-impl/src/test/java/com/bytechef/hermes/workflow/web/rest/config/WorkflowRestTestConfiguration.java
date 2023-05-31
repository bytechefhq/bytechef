
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

package com.bytechef.hermes.workflow.web.rest.config;

import com.bytechef.hermes.workflow.facade.TaskExecutionFacade;
import com.bytechef.atlas.factory.JobFactory;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.hermes.workflow.facade.WorkflowFacade;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.bytechef.hermes.workflow.web.rest")
@SpringBootConfiguration
public class WorkflowRestTestConfiguration {

    @MockBean
    private JobFactory jobFactory;

    @MockBean
    private JobService jobService;

    @MockBean
    private MessageBroker messageBroker;

    @MockBean
    private TaskExecutionService taskExecutionService;

    @MockBean
    private TaskExecutionFacade taskExecutionFacade;

    @MockBean
    private WorkflowService workflowService;

    @MockBean
    WorkflowFacade workflowFacade;
}
