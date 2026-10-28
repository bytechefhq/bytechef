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

package com.bytechef.platform.user.job;

import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the nightly user cleanup once per tenant. Each cleanup opens its own transaction after the tenant is set, so it
 * reads and deletes rows in that tenant's schema.
 *
 * @author Ivica Cardic
 */
@Component
public class UserCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UserCleanupJob.class);

    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public UserCleanupJob(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    /**
     * Not activated users are deleted 3 days after registration, every day at 01:00.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        runForEachTenant(userService::removeNotActivatedUsers);
    }

    /**
     * Persistent tokens provide automatic authentication and are deleted once they expire, every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        runForEachTenant(userService::removeOldPersistentTokens);
    }

    private void runForEachTenant(Runnable cleanup) {
        if (!tenantService.isMultiTenantEnabled()) {
            cleanup.run();

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, cleanup::run);
            } catch (RuntimeException exception) {
                log.error("User cleanup failed for tenant {}", tenantId, exception);
            }
        }
    }
}
