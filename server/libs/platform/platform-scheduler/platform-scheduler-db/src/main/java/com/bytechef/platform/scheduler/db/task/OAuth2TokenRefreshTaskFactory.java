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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.OAUTH2_TOKEN_REFRESH;

import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.github.kagkarlsson.scheduler.task.FailureHandler;
import com.github.kagkarlsson.scheduler.task.helper.CustomTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Ivica Cardic
 */
public final class OAuth2TokenRefreshTaskFactory {

    static final Duration TOKEN_REFRESH_OFFSET = Duration.ofMinutes(5);
    static final Duration RETRY_DELAY = Duration.ofMinutes(5);
    static final int MAX_RETRIES = 3;

    private OAuth2TokenRefreshTaskFactory() {
    }

    public static CustomTask<OAuth2TokenRefreshData> create(ConnectionFacade connectionFacade) {
        return Tasks.custom(OAUTH2_TOKEN_REFRESH)
            .onFailure(new FailureHandler.MaxRetriesFailureHandler<>(
                MAX_RETRIES, new FailureHandler.OnFailureRetryLater<>(RETRY_DELAY)))
            .execute((taskInstance, executionContext) -> {
                OAuth2TokenRefreshData data = taskInstance.getData();

                Integer expiresIn = ContextBinding.call(
                    data.tenantId(), () -> connectionFacade.executeConnectionRefresh(data.connectionId()));

                Instant nextRefresh = Instant.now()
                    .plusSeconds(expiresIn)
                    .minus(TOKEN_REFRESH_OFFSET);

                return new RescheduleAt<>(nextRefresh);
            });
    }
}
