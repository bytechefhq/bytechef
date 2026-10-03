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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public final class ExpiredSessionCleaner {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSessionCleaner.class);

    private final SessionRepository sessionRepository;
    private final Supplier<List<String>> tenantIdsSupplier;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ExpiredSessionCleaner(SessionRepository sessionRepository, Supplier<List<String>> tenantIdsSupplier) {
        this.sessionRepository = sessionRepository;
        this.tenantIdsSupplier = tenantIdsSupplier;
    }

    public int deleteExpiredSessions(Instant before) {
        int deletedCount = 0;

        for (String tenantId : tenantIdsSupplier.get()) {
            try {
                int tenantDeletedCount = TenantContext.callWithTenantId(
                    tenantId, () -> sessionRepository.deleteExpiredSessions(before));

                if (tenantDeletedCount > 0) {
                    log.info("Deleted {} expired sessions for tenant {}", tenantDeletedCount, tenantId);
                }

                deletedCount += tenantDeletedCount;
            } catch (RuntimeException runtimeException) {
                log.error("Failed to delete expired sessions for tenant {}", tenantId, runtimeException);
            }
        }

        return deletedCount;
    }
}
