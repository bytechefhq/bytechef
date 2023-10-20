
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

package com.bytechef.component.schedule.constant;

import com.bytechef.component.schedule.data.WorkflowScheduleAndData;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;

/**
 * @author Ivica Cardic
 */
public class ScheduleConstants {

    public static final String EXPRESSION = "expression";
    public static final String DAY_OF_MONTH = "dayOfMonth";
    public static final String DAY_OF_WEEK = "dayOfWeek";
    public static final String DATETIME = "datetime";
    public static final String HOUR = "hour";
    public static final String INTERVAL = "interval";
    public static final String MINUTE = "minute";
    public static final String SCHEDULE = "schedule";
    public static final String TIME_UNIT = "timeUnit";
    public static final String TIMEZONE = "timezone";

    public static final TaskWithDataDescriptor<WorkflowScheduleAndData> SCHEDULE_RECURRING_TASK =
        new TaskWithDataDescriptor<>("schedule-recurring-task", WorkflowScheduleAndData.class);
}
