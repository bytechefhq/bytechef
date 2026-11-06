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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.ONE_TIME_RESUME;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public final class OneTimeResumeTaskFactory {

    private OneTimeResumeTaskFactory() {
    }

    public static Task<OneTimeResumeData> create(ApplicationEventPublisher eventPublisher) {
        return Tasks.oneTime(ONE_TIME_RESUME)
            .execute((taskInstance, executionContext) -> {
                OneTimeResumeData data = taskInstance.getData();
                Runnable publishResumeEvent = () -> eventPublisher.publishEvent(new ResumeJobEvent(data.jobId()));

                if (Objects.nonNull(data.tenantId())) {
                    ContextBinding.run(data.tenantId(), publishResumeEvent);
                } else {
                    ContextBinding.runAsSystem(publishResumeEvent);
                }
            });
    }
}
