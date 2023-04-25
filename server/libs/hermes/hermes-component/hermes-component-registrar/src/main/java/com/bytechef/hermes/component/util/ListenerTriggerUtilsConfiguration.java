
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

package com.bytechef.hermes.component.util;

import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ListenerTriggerUtilsConfiguration implements InitializingBean {

    private final MessageBroker messageBroker;

    public ListenerTriggerUtilsConfiguration(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    @SuppressFBWarnings("ST")
    public void afterPropertiesSet() {
        ListenerTriggerUtils.listenerEmitter = (workflowExecutionId, output) -> {

            TriggerExecution triggerExecution = TriggerExecution.builder()
                .output(output)
                .workflowExecutionId(WorkflowExecutionId.parse(workflowExecutionId))
                .build();

            messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, triggerExecution);
        };
    }
}
