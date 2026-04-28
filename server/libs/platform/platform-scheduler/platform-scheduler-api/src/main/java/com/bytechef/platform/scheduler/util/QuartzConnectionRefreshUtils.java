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

package com.bytechef.platform.scheduler.util;

import static com.bytechef.platform.scheduler.constant.QuartzConnectionRefreshConstant.CONNECTION_OAUTH_2_TOKEN_REFRESH;
import static com.bytechef.platform.scheduler.constant.QuartzConnectionRefreshConstant.TOKEN_REFRESH_OFFSET_MINUTES;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

/**
 * @author Nikolina Spehar
 */
public class QuartzConnectionRefreshUtils {

    private QuartzConnectionRefreshUtils() {
    }

    public static JobKey buildJobKey(Long connectionId, String tenantId) {
        return JobKey.jobKey(tenantId + connectionId, CONNECTION_OAUTH_2_TOKEN_REFRESH);
    }

    public static Trigger buildRefreshTrigger(TriggerKey triggerKey, Long connectionId, Instant triggerTime) {
        return TriggerBuilder.newTrigger()
            .withIdentity(triggerKey)
            .withDescription("Connection OAuth2 token refresh for " + connectionId)
            .startAt(Date.from(triggerTime.minus(Duration.ofMinutes(TOKEN_REFRESH_OFFSET_MINUTES))))
            .build();
    }

    public static TriggerKey buildTriggerKey(Long connectionId, String tenantId) {
        return TriggerKey.triggerKey(tenantId + connectionId, CONNECTION_OAUTH_2_TOKEN_REFRESH);
    }
}
