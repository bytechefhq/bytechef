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

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nikolina Spehar
 */
@Component
public class ConnectionOAuth2TokenRefreshJob implements Job {

    @Autowired
    private ConnectionDefinitionFacade connectionDefinitionFacade;

    public ConnectionOAuth2TokenRefreshJob() {
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        Long connectionId = jobDataMap.getLong("connectionId");

        ComponentConnection componentConnection = connectionDefinitionFacade.executeConnectionRefresh(connectionId);

        Scheduler scheduler = context.getScheduler();

        try {
            Map<String, ?> parameters = componentConnection.getParameters();

            Long expiresIn = (Long) parameters.get("expires_in");

            Instant nextTriggerTime = Instant.now()
                .plusSeconds(expiresIn);

            scheduler.rescheduleJob(
                context.getTrigger()
                    .getKey(),
                TriggerBuilder.newTrigger()
                    .withIdentity(context.getTrigger()
                        .getKey())
                    .withDescription("Connection OAuth2 token refresh for " + connectionId)
                    .startAt(Date.from(nextTriggerTime.minus(Duration.ofMinutes(5))))
                    .build());
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
    }
}
