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
 * Removes expired invitations and pending sign-ups every day, at 01:00 (am), in the default schema and, in multi-tenant
 * mode, in every tenant schema.
 *
 * @author Ivica Cardic
 */
@Component
public class NotActivatedUserCleanup {

    private static final Logger log = LoggerFactory.getLogger(NotActivatedUserCleanup.class);

    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public NotActivatedUserCleanup(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userService.removeNotActivatedUsers();

        if (!tenantService.isMultiTenantEnabled()) {
            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, userService::removeNotActivatedUsers);
            } catch (RuntimeException exception) {
                log.error("Removing not activated users of tenant {} failed", tenantId, exception);
            }
        }
    }
}
