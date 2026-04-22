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

package com.bytechef.platform.scheduler;

/**
 * Schedules recurring AI Gateway observability export jobs. Implementations persist a cron-triggered job keyed by
 * {@code exportJobId}; firing publishes an {@link com.bytechef.platform.scheduler.event.ExportExecutionEvent} that the
 * gateway's export executor consumes.
 *
 * @author Ivica Cardic
 */
public interface ExportScheduler {

    void scheduleExport(long exportJobId, String cronExpression);

    void cancelExport(long exportJobId);
}
