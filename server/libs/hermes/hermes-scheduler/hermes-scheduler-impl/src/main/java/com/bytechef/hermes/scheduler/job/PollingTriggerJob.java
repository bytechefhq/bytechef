
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

package com.bytechef.hermes.scheduler.job;

import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
public class PollingTriggerJob implements Job {

    private MessageBroker messageBroker;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();

        messageBroker.send(TriggerMessageRoute.POLLS, jobDataMap.getString("workflowExecutionId"));
    }

    @Autowired
    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }
}
