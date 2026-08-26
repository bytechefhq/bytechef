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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * The dedup state is static and lives for the JVM, so each test resets it via {@code clearDedupState()} first.
 *
 * @author Ivica Cardic
 */
class ResourceMembershipGovernanceGapLogTest {

    private ListAppender<ILoggingEvent> logAppender;

    // Not a `private static final log`: this is a mutable handle to the logback logger under test, so the appender can
    // be attached and its level flipped per test. Same shape and same suppression as ResourceMembershipDenialLogTest.
    @SuppressWarnings("PMD")
    private ch.qos.logback.classic.Logger gapLogLogger;

    private Level originalLevel;

    @BeforeEach
    void setUp() {
        ResourceMembershipGovernanceGapLog.clearDedupState();

        gapLogLogger = (ch.qos.logback.classic.Logger) LoggerFactory
            .getLogger(ResourceMembershipGovernanceGapLog.class);
        logAppender = new ListAppender<>();

        originalLevel = gapLogLogger.getLevel();

        logAppender.start();

        gapLogLogger.addAppender(logAppender);
        gapLogLogger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        logAppender.stop();

        gapLogLogger.setLevel(originalLevel);

        gapLogLogger.detachAppender(logAppender);

        ResourceMembershipGovernanceGapLog.clearDedupState();
    }

    @Test
    void testEnvironmentBearingPrincipalLogsAtWarn() {
        authenticateAsApiKey("connected-user-1");

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("connected-user-1", "Workflow", "workflow-1");
    }

    /**
     * A session principal reaches {@code NOT_GOVERNED} on every single resource-scoped check it ever makes — that is
     * its normal path, not an anomaly. Logging it would bury the signal under the platform's entire traffic.
     */
    @Test
    void testSessionPrincipalIsSilent() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken("platform-user", "", List.of()));

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).isEmpty();
    }

    /**
     * Five of the eight {@code AbstractApiKeyAuthenticationToken} subclasses are built from the {@code User}-only super
     * constructor and carry no environment, so {@code ResourceMembershipResolver} cannot govern them and is not meant
     * to. Under the earlier "is this an api-key principal" predicate every public-API call they made logged a WARN
     * announcing a 403 that never happened.
     */
    @Test
    void testApiKeyPrincipalWithNoEnvironmentIsSilent() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestEnvironmentlessApiKeyAuthenticationToken("public-api-caller"));

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("project-1", "Project", "PROJECT_READ");

        assertThat(logAppender.list).isEmpty();
    }

    /**
     * An armed skip mode means the check was granted, not denied, so there is no 403 to announce and this log stays
     * quiet.
     */
    @Test
    void testArmedSkipModeIsSilent() throws Throwable {
        authenticateAsApiKey("connected-user-1");

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

            return null;
        });

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void testUnauthenticatedThreadIsSilent() {
        SecurityContextHolder.clearContext();

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).isEmpty();
    }

    /**
     * The dedup key is the principal alone, so a caller walking ids reports once rather than once per id. That is both
     * the useful answer — "this principal is not recognised" — and the property that stops the log being drivable by
     * caller-supplied input.
     */
    @Test
    void testTheSamePrincipalIsReportedOncePerJvmAcrossDifferentIds() {
        authenticateAsApiKey("connected-user-1");

        for (int index = 0; index < 50; index++) {
            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-" + index, "Workflow",
                "WORKFLOW_EDIT");
        }

        assertThat(logAppender.list).hasSize(1);
    }

    @Test
    void testEachDistinctPrincipalIsReportedOnce() {
        authenticateAsApiKey("connected-user-1");

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

        authenticateAsApiKey("connected-user-2");

        ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");

        assertThat(logAppender.list).hasSize(2);
    }

    /**
     * Past the bound, per-occurrence logging drops to DEBUG and must STAY there. Resuming per-request WARNs would hand
     * a caller minting fresh external ids control of the WARN volume — the log would amplify the probing it exists to
     * report.
     */
    @Test
    void testPastTheBoundPerOccurrenceLoggingGoesQuiet() {
        fillDedupSetToItsBound();

        gapLogLogger.setLevel(Level.WARN);

        logAppender.list.clear();

        for (int index = 0; index < 50; index++) {
            authenticateAsApiKey("past-bound-" + index);

            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");
        }

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void testAPeriodicAggregateWarnStillReportsSuppressedGaps() {
        fillDedupSetToItsBound();

        gapLogLogger.setLevel(Level.WARN);

        logAppender.list.clear();

        for (int index = 0; index < 1_000; index++) {
            authenticateAsApiKey("aggregate-" + index);

            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");
        }

        assertThat(logAppender.list).hasSize(1);

        ILoggingEvent event = logAppender.list.get(0);

        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("further occurrences logged");
    }

    /**
     * Adds enough distinct principals to take the dedup set past {@code MAX_TRACKED_PRINCIPALS}. Deliberately does not
     * read that constant — 1,001 additions exceed it from any starting point, and the test should not have to be edited
     * when the bound is tuned.
     */
    private static void fillDedupSetToItsBound() {
        for (int index = 0; index < 1_001; index++) {
            authenticateAsApiKey("fill-" + index);

            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected("workflow-1", "Workflow", "WORKFLOW_EDIT");
        }
    }

    private static void authenticateAsApiKey(String externalUserId) {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestApiKeyAuthenticationToken(externalUserId));
    }

    /**
     * Carries an environment, as the two token families this seam governs do.
     */
    private static class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestApiKeyAuthenticationToken(String externalUserId) {
            super(0L, new User(externalUserId, "", List.of()));
        }
    }

    /**
     * Carries none, as the five families this seam is not meant to govern do.
     */
    private static class TestEnvironmentlessApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        TestEnvironmentlessApiKeyAuthenticationToken(String login) {
            super(new User(login, "", List.of()));
        }
    }
}
