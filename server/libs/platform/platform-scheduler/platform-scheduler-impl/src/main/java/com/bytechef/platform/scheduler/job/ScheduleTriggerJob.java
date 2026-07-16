/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.scheduler.job;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class ScheduleTriggerJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTriggerJob.class);
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Date fireTime = context.getFireTime();

        Map<String, Object> output = JsonUtils.readMap(jobDataMap.getString("output"), Object.class);

        LocalDateTime localDateTime = fireTime.toInstant()
            .atZone(getZoneId(output))
            .toLocalDateTime();

        eventPublisher.publishEvent(
            new TriggerListenerEvent(
                new TriggerListenerEvent.ListenerParameters(
                    WorkflowExecutionId.parse(jobDataMap.getString("workflowExecutionId")),
                    fireTime.toInstant(),
                    MapUtils.concat(Map.of("fireTime", fireTime, "dateTime", localDateTime), output))));
    }

    @Autowired
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * This method returns the default ZoneId if the key or value miss. Method mitigates NullPointerException in the
     * cases when we upgrade action with the new zoneId required parameter which old definitions present in production
     * don't have. This method can be removed once you confirm there are no workflow definitions that miss this value.
     *
     * @param map
     * @return configured value or default given by defaultValue argument
     */
    private static ZoneId getZoneId(Map<String, ?> map) {
        if (map.containsKey("timezone")) {
            Object value = map.get("timezone");

            if (value instanceof String stringValue) {
                return ZoneId.of(stringValue);
            }
        }

        return ZoneId.systemDefault();
    }

}
