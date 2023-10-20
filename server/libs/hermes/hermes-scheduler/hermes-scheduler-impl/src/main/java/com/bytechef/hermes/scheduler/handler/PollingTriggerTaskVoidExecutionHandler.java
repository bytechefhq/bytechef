
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

package com.bytechef.hermes.scheduler.handler;

import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.scheduler.trigger.data.PollingTriggerScheduleAndData;
import com.bytechef.message.broker.MessageBroker;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.VoidExecutionHandler;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
public class PollingTriggerTaskVoidExecutionHandler implements VoidExecutionHandler<PollingTriggerScheduleAndData> {

    private final ApplicationContext applicationContext;

    public PollingTriggerTaskVoidExecutionHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void execute(TaskInstance<PollingTriggerScheduleAndData> taskInstance, ExecutionContext executionContext) {
        PollingTriggerScheduleAndData triggerScheduleAndData = taskInstance.getData();

        MessageBroker messageBroker = applicationContext.getBean(MessageBroker.class);

        messageBroker.send(TriggerMessageRoute.POLLS, triggerScheduleAndData.getData());
    }
}
