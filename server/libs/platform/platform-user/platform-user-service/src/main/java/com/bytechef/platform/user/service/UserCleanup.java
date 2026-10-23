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

package com.bytechef.platform.user.service;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the nightly user cleanups in the default schema and, in multi-tenant mode, in every tenant schema: old
 * persistent (remember-me) tokens at midnight, expired invitations and pending sign-ups at 01:00.
 *
 * @author Ivica Cardic
 */
@Component
public class UserCleanup {

    private static final Logger log = LoggerFactory.getLogger(UserCleanup.class);

    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public UserCleanup(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        runInEveryTenant("not activated users", userService::removeNotActivatedUsers);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void removeOldPersistentTokens() {
        runInEveryTenant("old persistent tokens", userService::removeOldPersistentTokens);
    }

    private void runInEveryTenant(String cleanupName, Runnable cleanup) {
        cleanup.run();

        if (!tenantService.isMultiTenantEnabled()) {
            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, cleanup::run);
            } catch (RuntimeException exception) {
                log.error("Removing {} of tenant {} failed", cleanupName, tenantId, exception);
            }
        }
    }
}
