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

package com.bytechef.platform.scheduler.db.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.CronStyle;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import java.time.ZoneId;

/**
 * @author Ivica Cardic
 */
public record ScheduleTriggerData(String cronPattern, String zoneId, String output) implements ScheduleAndData {

    @JsonIgnore
    @Override
    public Schedule getSchedule() {
        return new CronSchedule(cronPattern, ZoneId.of(zoneId), CronStyle.QUARTZ);
    }

    @JsonIgnore
    @Override
    public Object getData() {
        return output;
    }
}
