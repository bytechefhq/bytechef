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

package com.bytechef.platform.user.scheduler;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the periodic user cleanup jobs on coordinator nodes only.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
public class UserCleanupScheduler {

    private final UserService userService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UserCleanupScheduler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Deletes expired persistent tokens. Fires every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        userService.removeOldPersistentTokens();
    }

    /**
     * Deletes users that were never activated. Fires every day at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userService.removeNotActivatedUsers();
    }
}
