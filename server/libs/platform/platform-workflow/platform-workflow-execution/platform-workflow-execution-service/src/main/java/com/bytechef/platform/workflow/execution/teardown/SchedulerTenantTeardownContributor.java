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

package com.bytechef.platform.workflow.execution.teardown;

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sweeps the scheduler for trigger and connection refresh jobs of the tenant that disabling its deployments and
 * instances left behind.
 *
 * @author Ivica Cardic
 */
@Component
@Order(400)
class SchedulerTenantTeardownContributor implements TenantTeardownContributor {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTenantTeardownContributor.class);

    private final ConnectionRefreshScheduler connectionRefreshScheduler;
    private final TriggerScheduler triggerScheduler;

    @SuppressFBWarnings("EI")
    SchedulerTenantTeardownContributor(
        ConnectionRefreshScheduler connectionRefreshScheduler, TriggerScheduler triggerScheduler) {

        this.connectionRefreshScheduler = connectionRefreshScheduler;
        this.triggerScheduler = triggerScheduler;
    }

    @Override
    public List<String> onDeletionRequested(String tenantId) {
        List<String> failures = new ArrayList<>();

        try {
            triggerScheduler.cancelAllForTenant(tenantId);
        } catch (RuntimeException e) {
            log.error("Unable to cancel the trigger jobs of tenant {}", tenantId, e);

            failures.add("triggerScheduler: " + e.getMessage());
        }

        try {
            connectionRefreshScheduler.cancelAllForTenant(tenantId);
        } catch (RuntimeException e) {
            log.error("Unable to cancel the connection refresh jobs of tenant {}", tenantId, e);

            failures.add("connectionRefreshScheduler: " + e.getMessage());
        }

        return failures;
    }
}
