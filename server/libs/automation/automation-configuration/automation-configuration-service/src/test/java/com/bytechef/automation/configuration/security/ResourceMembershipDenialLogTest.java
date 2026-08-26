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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

/**
 * {@link ResourceMembershipDenialLog}'s dedup set is a private static field shared across every test in this JVM, so
 * each test here resets it via {@code clearDedupState()} first. The tests below still use distinct (resourceType, id)
 * pairs, which costs nothing and keeps them readable in isolation. The dedup behavior itself is exercised explicitly in
 * {@link #testSecondIdenticalCallIsNotLoggedAgain()} and {@link #testADistinctIdLogsAgainAfterADuplicate()}.
 *
 * @author Ivica Cardic
 */
class ResourceMembershipDenialLogTest {

    private static final String EXTERNAL_USER_ID = "connected-user-denial-log-test";

    private ListAppender<ILoggingEvent> logAppender;

    @SuppressWarnings("PMD")
    private ch.qos.logback.classic.Logger denialLogLogger;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private Level originalLevel;

    @BeforeEach
    void setUp() {
        ResourceMembershipDenialLog.clearDedupState();

        denialLogLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ResourceMembershipDenialLog.class);
        logAppender = new ListAppender<>();

        originalLevel = denialLogLogger.getLevel();

        logAppender.start();
        denialLogLogger.addAppender(logAppender);

        securityUtilsMock = mockStatic(SecurityUtils.class);

        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(EXTERNAL_USER_ID));
    }

    @AfterEach
    void tearDown() {
        // Restored because the logger is a JVM-global object: leaving it at DEBUG would make the WARN-only assertions
        // in the other tests here capture DEBUG events too, depending on execution order.
        denialLogLogger.setLevel(originalLevel);

        denialLogLogger.detachAppender(logAppender);
        securityUtilsMock.close();

        ResourceMembershipDenialLog.clearDedupState();
    }

    // -- Ticket 1051 fix round: past the dedup bound the log must not be drivable ----------------------------------

    /**
     * Before this fix, once the dedup set reached its bound {@code add} was never called again and EVERY subsequent
     * denial logged at WARN. That was tolerable while a denial was rare by construction (shadow mode); with enforcement
     * a denial is the EXPECTED outcome of any id probing by an authenticated connected user, so a single caller walking
     * ids could drive unbounded WARN volume -- turning the log into the amplifier for the abuse it is meant to record.
     */
    @Test
    void testDenialsPastTheDedupBoundDropToDebugInsteadOfWarningEveryTime() {
        denialLogLogger.setLevel(Level.DEBUG);

        fillDedupSetToItsBound();

        logAppender.list.clear();

        for (int index = 0; index < 50; index++) {
            ResourceMembershipDenialLog.logIfNoteworthy(
                Decision.DENIED, "past-bound-" + index, "Workflow", "WORKFLOW_EDIT");
        }

        assertThat(logAppender.list)
            .as("every denial past the bound must be DEBUG, none WARN")
            .hasSize(50)
            .allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.DEBUG));
    }

    /**
     * Dropping to DEBUG must not make the situation invisible: one aggregate WARN per
     * {@code SUPPRESSED_REPORT_INTERVAL} denials keeps it on the operator's radar at a volume the caller cannot choose.
     */
    @Test
    void testAPeriodicAggregateWarnStillReportsSuppressedDenials() {
        denialLogLogger.setLevel(Level.WARN);

        fillDedupSetToItsBound();

        logAppender.list.clear();

        for (int index = 0; index < 1_000; index++) {
            ResourceMembershipDenialLog.logIfNoteworthy(
                Decision.DENIED, "aggregate-" + index, "Workflow", "WORKFLOW_EDIT");
        }

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("further denials logged");
    }

    /**
     * Adds enough distinct tuples to take the dedup set past {@code MAX_TRACKED_TUPLES}. Deliberately does not read
     * that constant -- 10,001 additions exceed it from any starting point, and the test should not have to be edited
     * when the bound is tuned.
     */
    private static void fillDedupSetToItsBound() {
        for (int index = 0; index < 10_001; index++) {
            ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "fill-" + index, "Workflow", "WORKFLOW_EDIT");
        }
    }

    @Test
    void testDeniedLogsAtWarn() {
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-denied-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()
            .toString()).isEqualTo("WARN");
        assertThat(event.getFormattedMessage()).contains(
            EXTERNAL_USER_ID, "Workflow", "workflow-denied-1", "WORKFLOW_EDIT", "DENIED");
    }

    @Test
    void testGovernedNotApplicableLogsAtWarn() {
        ResourceMembershipDenialLog.logIfNoteworthy(
            Decision.NOT_APPLICABLE, "workflow-not-applicable-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0)
            .getFormattedMessage()).contains("NOT_APPLICABLE");
    }

    @Test
    void testGrantedNeverLogs() {
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.GRANTED, "workflow-granted-1", "Workflow",
            "WORKFLOW_EDIT");

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void testSecondIdenticalCallIsNotLoggedAgain() {
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-1", "Workflow", "WORKFLOW_EDIT");
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-1", "Workflow", "WORKFLOW_EDIT");
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(1);
    }

    @Test
    void testADistinctIdLogsAgainAfterADuplicate() {
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-2a", "Workflow", "WORKFLOW_EDIT");
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-2a", "Workflow", "WORKFLOW_EDIT");
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-2b", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(2);
    }

    @Test
    void testADifferentDecisionForTheSameIdLogsAgain() {
        // Same resource, same scope, different decision -- e.g. a resolver bug fix mid-session flips DENIED to
        // NOT_APPLICABLE. That is a materially different fact and must not be swallowed by the dedup key.
        ResourceMembershipDenialLog.logIfNoteworthy(Decision.DENIED, "workflow-dedup-3", "Workflow", "WORKFLOW_EDIT");
        ResourceMembershipDenialLog.logIfNoteworthy(
            Decision.NOT_APPLICABLE, "workflow-dedup-3", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(2);
    }
}
