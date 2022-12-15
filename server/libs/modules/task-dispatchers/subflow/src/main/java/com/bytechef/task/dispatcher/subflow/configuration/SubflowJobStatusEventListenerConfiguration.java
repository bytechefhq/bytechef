
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

package com.bytechef.task.dispatcher.subflow.configuration;

import com.bytechef.atlas.coordinator.CoordinatorManager;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.autoconfigure.annotation.ConditionalOnCoordinator;
import com.bytechef.task.dispatcher.subflow.event.SubflowJobStatusEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
public class SubflowJobStatusEventListenerConfiguration {

    @Autowired
    private CoordinatorManager coordinatorManager;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskEvaluator taskEvaluator;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Bean
    SubflowJobStatusEventListener subflowJobStatusEventListener() {
        return new SubflowJobStatusEventListener(jobService, taskExecutionService, coordinatorManager, taskEvaluator);
    }
}
