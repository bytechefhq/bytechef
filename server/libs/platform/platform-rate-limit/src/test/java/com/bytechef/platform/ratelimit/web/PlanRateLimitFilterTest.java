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

package com.bytechef.platform.ratelimit.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.ratelimit.RateLimiter;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the request-tier classification: webhook calls plus every MCP/A2A secret-key endpoint consume the per-tenant
 * sync bucket, while other authenticated API paths pass through untouched.
 *
 * @author Ivica Cardic
 */
public class PlanRateLimitFilterTest {

    private final PlanLimitRejectionCounter planLimitRejectionCounter = mock(PlanLimitRejectionCounter.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);

    private PlanRateLimitFilter planRateLimitFilter;

    @BeforeEach
    public void beforeEach() {
        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, null, 50, null, 30, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, Duration.ofMinutes(5), null,
            null, null, null, null);

        PlanLimitsProvider planLimitsProvider = tenantId -> planLimits;

        planRateLimitFilter = new PlanRateLimitFilter(planLimitRejectionCounter, planLimitsProvider, rateLimiter);

        // The pre-auth branch only applies to anonymous requests; authenticate so pass-through paths stay untouched.
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user", "n/a", "ROLE_USER"));

        when(rateLimiter.tryConsume(any(), any())).thenReturn(true);
    }

    @AfterEach
    public void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testMcpEndpointConsumesSyncBucket() throws ServletException, IOException {
        filter("/api/automation/a1b2c3/mcp");

        verify(rateLimiter).tryConsume(eq("sync:public"), any());
    }

    @Test
    public void testMcpSseAndMessageEndpointsConsumeSyncBucket() throws ServletException, IOException {
        filter("/api/embedded/a1b2c3/sse");
        filter("/api/management/a1b2c3/message");

        verify(rateLimiter, org.mockito.Mockito.times(2)).tryConsume(eq("sync:public"), any());
    }

    @Test
    public void testA2aEndpointConsumesSyncBucket() throws ServletException, IOException {
        filter("/api/automation/a2a/a1b2c3");

        verify(rateLimiter).tryConsume(eq("sync:public"), any());
    }

    @Test
    public void testInternalPathEndingInSseSegmentPassesThrough() throws ServletException, IOException {
        // Two segments before the tail — must not be mistaken for a secret-key MCP endpoint.
        MockFilterChain filterChain = filter("/api/automation/internal/workflow-chats/sse");

        verify(rateLimiter, org.mockito.Mockito.never()).tryConsume(any(), any());
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    public void testWebhookPathConsumesSyncBucket() throws ServletException, IOException {
        filter("/webhooks/a1b2c3");

        verify(rateLimiter).tryConsume(eq("sync:public"), any());
    }

    @Test
    public void testJobResumePathConsumesPerIpResumeBucket() throws ServletException, IOException {
        MockFilterChain filterChain = filter("/job/resume/v1.123.abc.sig");

        // Anonymous resume callbacks are metered per client IP, not per tenant.
        verify(rateLimiter).tryConsume(eq("resume:127.0.0.1"), any());
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    public void testJobResumePathRejectedWhenResumeBucketExhausted() throws ServletException, IOException {
        when(rateLimiter.tryConsume(eq("resume:127.0.0.1"), any())).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/job/resume/v1.123.abc.sig");

        request.setRequestURI("/job/resume/v1.123.abc.sig");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        planRateLimitFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(filterChain.getRequest()).isNull();
    }

    private MockFilterChain filter(String path) throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);

        request.setRequestURI(path);

        MockFilterChain filterChain = new MockFilterChain();

        planRateLimitFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

        return filterChain;
    }
}
