
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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScheduleTriggerJob implements Job {

    private MessageBroker messageBroker;
    private ObjectMapper objectMapper;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Date fireTime = context.getFireTime();

        messageBroker.send(
            TriggerMessageRoute.LISTENERS,
            new ListenerParameters(
                WorkflowExecutionId.parse(jobDataMap.getString("workflowExecutionId")),
                MapUtils.concat(
                    Map.of("datetime", fireTime.toString()),
                    JsonUtils.readMap(jobDataMap.getString("output"), String.class, objectMapper))));
    }

    private record ListenerParameters(WorkflowExecutionId workflowExecutionId, Object output) {
    }

    @Autowired
    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Autowired
    @SuppressFBWarnings("EI")
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
