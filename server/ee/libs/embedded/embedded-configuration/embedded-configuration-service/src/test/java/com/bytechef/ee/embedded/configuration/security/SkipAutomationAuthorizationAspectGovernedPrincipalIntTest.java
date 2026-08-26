/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import java.io.Serializable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

/**
 * {@code SkipAutomationAuthorizationAspect} with a {@link ResourceMembershipResolver} present — the Enterprise embedded
 * deployment. The sibling {@code SkipAutomationAuthorizationAspectIntTest} covers the no-resolver context (Community
 * Edition), where the aspect must keep arming full skip exactly as it always did.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = SkipAutomationAuthorizationAspectGovernedPrincipalIntTest.Config.class)
@TestPropertySource(properties = "bytechef.edition=ee")
class SkipAutomationAuthorizationAspectGovernedPrincipalIntTest {

    @Autowired
    private AnnotatedBridge annotatedBridge;

    @Autowired
    private ControllableResourceMembershipResolver resourceMembershipResolver;

    @BeforeEach
    @AfterEach
    void assertCleanState() {
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @BeforeEach
    void resetResolver() {
        resourceMembershipResolver.setGoverning(false);
        resourceMembershipResolver.setFailure(null);
    }

    @Test
    void testUngovernedPrincipalStillGetsFullSkip() {
        resourceMembershipResolver.setGoverning(false);

        assertThat(annotatedBridge.captureSkipDuringCall()).isTrue();
    }

    @Test
    void testGovernedPrincipalGetsNoSkipAtAll() {
        resourceMembershipResolver.setGoverning(true);

        assertThat(annotatedBridge.captureSkipDuringCall()).isFalse();
    }

    /**
     * Deliberately re-asserts {@link #testGovernedPrincipalGetsNoSkipAtAll()} rather than sharing it. That test says
     * "no skip is armed", which is an implementation statement and could legitimately be relaxed; this one names the
     * security property, so it should fail on its own terms and under its own name if the aspect ever starts arming
     * skip mode for a governed principal again.
     *
     * <p>
     * Skip mode grants {@code isTenantAdmin()}, workspace membership, current-user identity and resource ownership —
     * none of which an embedded connected user has. The property that used to be held by the monotonic-narrowing rule
     * in {@code AutomationAuthorizationContext}, itself resting on a thread-local floor that
     * {@code EmbeddedAutomationAuthorizationSkipFilter} had to arm first, is now held by the principal. That is what
     * makes it hold on {@code configuration-app} and on the embedded MCP chain, where no filter has ever run, and on
     * the copilot worker threads, where no filter could. Both the filter and the narrowing rule are gone; this
     * assertion is what is left, and it must keep passing.
     */
    @Test
    void testGovernedPrincipalIsNeverGrantedTenantAdminLevelSkip() {
        resourceMembershipResolver.setGoverning(true);

        assertThat(annotatedBridge.captureSkipDuringCall()).isFalse();
    }

    @Test
    void testNestedAnnotatedCallKeepsGovernedPrincipalUnskipped() {
        resourceMembershipResolver.setGoverning(true);

        assertThat(annotatedBridge.captureSkipDuringNestedCall()).isFalse();
    }

    @Test
    void testGovernanceIsReadPerInvocationRatherThanOnce() {
        resourceMembershipResolver.setGoverning(true);

        assertThat(annotatedBridge.captureSkipDuringCall()).isFalse();

        resourceMembershipResolver.setGoverning(false);

        assertThat(annotatedBridge.captureSkipDuringCall()).isTrue();
    }

    /**
     * The aspect must let a resolver failure out rather than falling through to {@code callSkippingChecks}. Answering
     * "not governed" on a failed lookup would arm full skip for a caller that may well be a connected user — trading a
     * visible 500 for a silent grant of tenant-admin status, which is the one outcome this aspect exists to prevent.
     * Documented at {@code SkipAutomationAuthorizationAspect.governsCurrentPrincipal}; pinned here.
     */
    @Test
    void testResolverFailurePropagatesRatherThanArmingFullSkip() {
        resourceMembershipResolver.setFailure(new IllegalStateException("connection pool exhausted"));

        assertThatThrownBy(() -> annotatedBridge.captureSkipDuringCall())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("connection pool exhausted");

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @Import({
        AnnotatedBridge.class, ControllableResourceMembershipResolver.class, NestedAnnotatedBridge.class,
        SkipAutomationAuthorizationAspect.class
    })
    static class Config {
    }

    @Service
    @SkipAutomationAuthorization
    static class AnnotatedBridge {

        private final NestedAnnotatedBridge nestedAnnotatedBridge;

        AnnotatedBridge(NestedAnnotatedBridge nestedAnnotatedBridge) {
            this.nestedAnnotatedBridge = nestedAnnotatedBridge;
        }

        public boolean captureSkipDuringCall() {
            return AutomationAuthorizationContext.isSkipChecks();
        }

        public boolean captureSkipDuringNestedCall() {
            return nestedAnnotatedBridge.captureSkipDuringCall();
        }
    }

    @Service
    @SkipAutomationAuthorization
    static class NestedAnnotatedBridge {

        public boolean captureSkipDuringCall() {
            return AutomationAuthorizationContext.isSkipChecks();
        }
    }

    /**
     * Stands in for {@code ConnectedUserResourceMembershipResolver}. Only {@link #governsCurrentPrincipal()} matters
     * here — the aspect never calls {@link #resolve(Serializable, String, String)}.
     */
    @Service
    static class ControllableResourceMembershipResolver implements ResourceMembershipResolver {

        private volatile boolean governing;

        private volatile RuntimeException failure;

        @Override
        public Decision resolve(Serializable id, String resourceType, String scope) {
            return Decision.NOT_APPLICABLE;
        }

        @Override
        public boolean governsCurrentPrincipal() {
            if (failure != null) {
                throw failure;
            }

            return governing;
        }

        void setGoverning(boolean governing) {
            this.governing = governing;
        }

        /**
         * Mirrors the real resolver, which as of ticket 1051 rethrows a failed {@code fetchConnectedUser} instead of
         * answering false.
         */
        void setFailure(RuntimeException failure) {
            this.failure = failure;
        }
    }
}
