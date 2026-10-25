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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
class UserCleanupSchedulerTest {

    @Test
    void testRemoveOldPersistentTokensDelegates() {
        UserService userService = mock(UserService.class);

        UserCleanupScheduler userCleanupScheduler = new UserCleanupScheduler(userService);

        userCleanupScheduler.removeOldPersistentTokens();

        verify(userService).removeOldPersistentTokens();
    }

    @Test
    void testRemoveNotActivatedUsersDelegates() {
        UserService userService = mock(UserService.class);

        UserCleanupScheduler userCleanupScheduler = new UserCleanupScheduler(userService);

        userCleanupScheduler.removeNotActivatedUsers();

        verify(userService).removeNotActivatedUsers();
    }

    @Test
    void testCronExpressionsAreUnchanged() throws NoSuchMethodException {
        Scheduled removeOldPersistentTokens = UserCleanupScheduler.class
            .getMethod("removeOldPersistentTokens")
            .getAnnotation(Scheduled.class);
        Scheduled removeNotActivatedUsers = UserCleanupScheduler.class
            .getMethod("removeNotActivatedUsers")
            .getAnnotation(Scheduled.class);

        assertThat(removeOldPersistentTokens.cron()).isEqualTo("0 0 0 * * ?");
        assertThat(removeNotActivatedUsers.cron()).isEqualTo("0 0 1 * * ?");
    }

    @Test
    void testRegisteredOnCoordinatorOnly() {
        ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UserCleanupScheduler.class)
            .withBean(UserService.class, () -> mock(UserService.class));

        applicationContextRunner.run(context -> assertThat(context).hasSingleBean(UserCleanupScheduler.class));

        applicationContextRunner.withPropertyValues("bytechef.coordinator.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(UserCleanupScheduler.class));
    }
}
