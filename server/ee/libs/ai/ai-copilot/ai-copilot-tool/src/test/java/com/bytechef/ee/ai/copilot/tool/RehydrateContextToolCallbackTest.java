/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 */
class RehydrateContextToolCallbackTest {

    private static final class RecordingRehydrator extends SecurityContextRehydrator {

        private Long observedUserId;

        private RecordingRehydrator() {
            super(null, null);
        }

        @Override
        public <T> T withUserSecurityContext(@Nullable Long userId, Supplier<T> action) {
            observedUserId = userId;

            return action.get();
        }
    }

    private static final class ProbeToolCallback implements ToolCallback {

        private String tenantSeenInside;
        private Authentication authenticationSeenInside;
        private boolean skipChecksSeenInside;

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("probe")
                .description("probe")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            tenantSeenInside = TenantContext.getCurrentTenantId();
            authenticationSeenInside = SecurityContextHolder.getContext()
                .getAuthentication();
            skipChecksSeenInside = AutomationAuthorizationContext.isSkipChecks();

            return "ok";
        }
    }

    @Test
    void testSetsTenantAndUserInsideCallThenRestores() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        ProbeToolCallback probe = new ProbeToolCallback();
        RecordingRehydrator rehydrator = new RecordingRehydrator();

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, rehydrator);

        Map<String, Object> map = new AgentToolInvocationContext(null, 42L, null, null, "acme").toToolContext();

        String result = wrapped.call("{}", new ToolContext(map));

        assertThat(result).isEqualTo("ok");
        assertThat(probe.tenantSeenInside).isEqualTo("acme");
        assertThat(rehydrator.observedUserId).isEqualTo(42L);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRestoresCapturedAuthenticationAndSkipsUserIdRehydration() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);
        SecurityContextHolder.clearContext();

        ProbeToolCallback probe = new ProbeToolCallback();
        RecordingRehydrator rehydrator = new RecordingRehydrator();

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, rehydrator);

        Authentication authentication = new UsernamePasswordAuthenticationToken("captured-user", "");

        Map<String, Object> map =
            new AgentToolInvocationContext(null, 42L, null, null, "acme", authentication).toToolContext();

        String result = wrapped.call("{}", new ToolContext(map));

        assertThat(result).isEqualTo("ok");
        assertThat(probe.tenantSeenInside).isEqualTo("acme");
        assertThat(probe.authenticationSeenInside).isSameAs(authentication);
        // The captured Authentication wins: the userId-based rehydrator is never consulted.
        assertThat(rehydrator.observedUserId).isNull();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(SecurityContextHolder.getContext()
            .getAuthentication()).isNull();
    }

    @Test
    void testReArmsSkipChecksInsideCallThenRestores() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);
        SecurityContextHolder.clearContext();

        ProbeToolCallback probe = new ProbeToolCallback();

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, new RecordingRehydrator());

        Authentication authentication = new UsernamePasswordAuthenticationToken("captured-user", "");

        Map<String, Object> map =
            new AgentToolInvocationContext(null, null, null, null, "acme", authentication, true).toToolContext();

        String result = wrapped.call("{}", new ToolContext(map));

        assertThat(result).isEqualTo("ok");
        // The embedded skip-authorization flag is re-armed on the worker thread for the duration of the delegate call.
        assertThat(probe.skipChecksSeenInside).isTrue();
        // ... and the ThreadLocal returns to its fail-closed default afterward.
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testDoesNotSkipChecksWhenFlagAbsent() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        ProbeToolCallback probe = new ProbeToolCallback();

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(probe, new RecordingRehydrator());

        Map<String, Object> map = new AgentToolInvocationContext(null, 42L, null, null, "acme").toToolContext();

        wrapped.call("{}", new ToolContext(map));

        assertThat(probe.skipChecksSeenInside).isFalse();
    }

    @Test
    void testIdempotentWrap() {
        RecordingRehydrator rehydrator = new RecordingRehydrator();
        ToolCallback wrapped = RehydrateContextToolCallback.wrap(new ProbeToolCallback(), rehydrator);

        assertThat(RehydrateContextToolCallback.wrap(wrapped, rehydrator)).isSameAs(wrapped);
    }

    @Test
    void testNoToolContextPassesThrough() {
        ToolCallback wrapped = RehydrateContextToolCallback.wrap(new ProbeToolCallback(), new RecordingRehydrator());

        assertThat(wrapped.call("{}")).isEqualTo("ok");
    }

    @Test
    void testTenantRestoredAndExceptionPropagatedOnDelegateThrow() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        ToolCallback throwingDelegate = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name("thrower")
                    .description("thrower")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                throw new RuntimeException("boom");
            }
        };

        ToolCallback wrapped = RehydrateContextToolCallback.wrap(throwingDelegate, new RecordingRehydrator());

        Map<String, Object> map = new AgentToolInvocationContext(null, 1L, null, null, "acme").toToolContext();

        assertThatThrownBy(() -> wrapped.call("{}", new ToolContext(map)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }
}
