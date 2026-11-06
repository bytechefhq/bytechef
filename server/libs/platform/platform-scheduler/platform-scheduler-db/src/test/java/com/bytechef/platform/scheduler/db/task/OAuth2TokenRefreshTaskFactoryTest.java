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

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import java.time.Duration;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OAuth2TokenRefreshTaskFactoryTest {

    @Test
    void testExecuteRefreshesAndReschedulesFiveMinutesBeforeExpiry() {
        ConnectionFacade connectionFacade = Mockito.mock(ConnectionFacade.class);

        Mockito.when(connectionFacade.executeConnectionRefresh(77L))
            .thenReturn(3600);

        CustomTask<OAuth2TokenRefreshData> task = OAuth2TokenRefreshTaskFactory.create(connectionFacade);
        TaskInstance<OAuth2TokenRefreshData> taskInstance = DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH
            .instance("00000177")
            .data(new OAuth2TokenRefreshData(77L, "000001"))
            .build();

        Instant before = Instant.now();
        CompletionHandler<OAuth2TokenRefreshData> completionHandler =
            task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, before));

        Assertions.assertThat(completionHandler)
            .isInstanceOf(RescheduleAt.class);

        Instant rescheduledTo = ((RescheduleAt<OAuth2TokenRefreshData>) completionHandler).executionTime();

        Assertions.assertThat(rescheduledTo)
            .isBetween(
                before.plusSeconds(3600)
                    .minus(Duration.ofMinutes(5)),
                Instant.now()
                    .plusSeconds(3600)
                    .minus(Duration.ofMinutes(5)));
    }
}
