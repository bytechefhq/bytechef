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

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.tenant.TenantContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class OneTimeResumeTaskFactoryTest {

    @Test
    void testExecutePublishesResumeJobEvent() {
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        Task<OneTimeResumeData> task = OneTimeResumeTaskFactory.create(eventPublisher);
        TaskInstance<OneTimeResumeData> taskInstance = DbSchedulerTaskDescriptors.ONE_TIME_RESUME
            .instance("42")
            .data(new OneTimeResumeData(42L, null, null))
            .build();

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, Instant.now()));

        ArgumentCaptor<ResumeJobEvent> captor = ArgumentCaptor.forClass(ResumeJobEvent.class);

        Mockito.verify(eventPublisher)
            .publishEvent(captor.capture());
        Assertions.assertThat(captor.getValue()
            .getJobId())
            .isEqualTo(42L);
    }

    @Test
    void testExecuteWithMissingTenantIdRunsAsSystemUnderDefaultTenant() {
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        Task<OneTimeResumeData> task = OneTimeResumeTaskFactory.create(eventPublisher);
        TaskInstance<OneTimeResumeData> taskInstance = DbSchedulerTaskDescriptors.ONE_TIME_RESUME
            .instance("42")
            .data(new OneTimeResumeData(42L, null, null))
            .build();

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, Instant.now()));

        Assertions.assertThat(TenantContext.getCurrentTenantId())
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testExecuteWithTenantIdBindsThatTenantWhileRunningAndRestoresItAfterward() {
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        Task<OneTimeResumeData> task = OneTimeResumeTaskFactory.create(eventPublisher);
        TaskInstance<OneTimeResumeData> taskInstance = DbSchedulerTaskDescriptors.ONE_TIME_RESUME
            .instance("44")
            .data(new OneTimeResumeData(44L, null, "tenant_1"))
            .build();

        Mockito.doAnswer(invocation -> {
            Assertions.assertThat(TenantContext.getCurrentTenantId())
                .isEqualTo("tenant_1");

            return null;
        })
            .when(eventPublisher)
            .publishEvent(Mockito.any(ResumeJobEvent.class));

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, Instant.now()));

        Assertions.assertThat(TenantContext.getCurrentTenantId())
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }
}
