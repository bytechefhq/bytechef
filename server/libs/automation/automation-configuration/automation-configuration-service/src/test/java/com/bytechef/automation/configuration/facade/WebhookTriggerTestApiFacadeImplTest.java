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

package com.bytechef.automation.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT') on both methods is environment-agnostic, so the
 * caller-supplied environmentId is never checked -- and it mints or tears down a live webhook URL in the environment it
 * names. These tests pin the execution side: for a confined (api-key) principal, the environment reaching the shared
 * platform facade must be the principal's own, not the request argument.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WebhookTriggerTestApiFacadeImplTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    @Mock
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    private WebhookTriggerTestApiFacadeImpl webhookTriggerTestApiFacade;

    @BeforeEach
    void setUp() {
        webhookTriggerTestApiFacade = new WebhookTriggerTestApiFacadeImpl(webhookTriggerTestFacade);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnableTriggerUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.AUTOMATION)))
            .thenReturn("https://example.org/webhook");

        webhookTriggerTestApiFacade.enableTrigger("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.AUTOMATION));

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testEnableTriggerHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(webhookTriggerTestFacade.enableTrigger(anyString(), anyLong(), eq(PlatformType.AUTOMATION)))
            .thenReturn("https://example.org/webhook");

        webhookTriggerTestApiFacade.enableTrigger("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).enableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.AUTOMATION));

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testDisableTriggerUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        webhookTriggerTestApiFacade.disableTrigger("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.AUTOMATION));

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testDisableTriggerHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        webhookTriggerTestApiFacade.disableTrigger("workflow-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(webhookTriggerTestFacade).disableTrigger(
            eq("workflow-1"), environmentIdCaptor.capture(), eq(PlatformType.AUTOMATION));

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
