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

package com.bytechef.automation.configuration.listener;

import com.bytechef.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to {@link WorkspaceUserRemovedEvent} by marking the removed user's owned workspace connections as
 * PENDING_REASSIGNMENT. Runs after the outer transaction commits so the removal is durable before status transitions.
 *
 * <p>
 * Because the removal transaction has already committed when the listener fires, any downstream failure leaves orphaned
 * credentials running under a nonexistent login. To make this condition alertable rather than hidden in logs, every
 * outcome increments {@code bytechef_connection_reassignment_listener} tagged with
 * {@code outcome=success|partial|error} so operators can alert on rising {@code partial}/{@code error} rates instead of
 * grepping logs.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkspaceUserRemovalListener {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceUserRemovalListener.class);

    private static final String METRIC_NAME = "bytechef_connection_reassignment_listener";
    private static final String METRIC_DESCRIPTION =
        "Outcome of the workspace-user-removal listener that flips owned connections to PENDING_REASSIGNMENT";

    private final ConnectionReassignmentFacade connectionReassignmentFacade;
    private final MeterRegistry meterRegistry;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public WorkspaceUserRemovalListener(
        ConnectionReassignmentFacade connectionReassignmentFacade,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.connectionReassignmentFacade = connectionReassignmentFacade;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkspaceUserRemoved(WorkspaceUserRemovedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug(
                "Marking connections owned by user='{}' in workspace={} as PENDING_REASSIGNMENT",
                event.userLogin(), event.workspaceId());
        }

        try {
            BulkReassignResultDTO result = connectionReassignmentFacade.markConnectionsPendingReassignment(
                event.workspaceId(), event.userLogin());

            if (result.failed() > 0) {
                // Partial failure: facade has already logged per-row errors. Surface a single WARN so
                // the listener's own observability does not silently claim success when some rows
                // did not transition. Alerting rules can key on this message or on the metric tag.
                logger.warn(
                    "markConnectionsPendingReassignment left {} connection(s) un-transitioned for removed user='{}' "
                        + "in workspace={}; operators should reconcile manually",
                    result.failed(), event.userLogin(), event.workspaceId());

                incrementMetric("partial");
            } else {
                incrementMetric("success");
            }
        } catch (RuntimeException exception) {
            incrementMetric("error");

            logger.error(
                "Failed to mark connections as PENDING_REASSIGNMENT for removed user='{}' in workspace={}. " +
                    "The user removal is already committed; operators should reconcile manually.",
                event.userLogin(), event.workspaceId(), exception);
        }
    }

    /**
     * Increments the outcome-tagged counter. The workspace identifier is intentionally NOT a tag: tenants can run with
     * thousands of workspaces, and a per-workspace time series would balloon Prometheus cardinality and break alerting.
     * The per-workspace detail is already in the WARN/ERROR log line above for operators that need to investigate a
     * specific incident.
     */
    private void incrementMetric(String outcome) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder(METRIC_NAME)
            .description(METRIC_DESCRIPTION)
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
