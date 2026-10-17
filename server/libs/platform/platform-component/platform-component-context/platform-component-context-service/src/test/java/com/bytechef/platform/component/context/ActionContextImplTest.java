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

package com.bytechef.platform.component.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ActionContextImplTest {

    @Test
    void testGetApprovalLinksSignsTheTokenUntilTheApprovalExpires() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured(anyString(), any(Duration.class)))
            .thenReturn(Optional.of("signed-token"));

        ActionContextImpl actionContext = createActionContext(approvalTokens);

        Instant now = Instant.now();

        Instant expiresAt = now.plus(Duration.ofDays(45));

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(expiresAt);

        assertNotNull(approvalLinks);
        assertEquals("signed-token", approvalLinks.resumeId());
        assertEquals("https://example.com/job/resume/signed-token", approvalLinks.resumeUrl());
        assertEquals("https://example.com/resume/signed-token", approvalLinks.formUrl());

        ArgumentCaptor<Duration> ttlArgumentCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(approvalTokens).toSignedTokenIfConfigured(
            eq(actionContext.getJobResumeId()), ttlArgumentCaptor.capture());

        Duration ttl = ttlArgumentCaptor.getValue();

        assertTrue(ttl.compareTo(Duration.ofDays(45)) > 0);
        assertTrue(ttl.compareTo(Duration.ofDays(46)) < 0);
    }

    @Test
    void testGetApprovalLinksShortensTheSignedResumeId() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured(anyString(), any(Duration.class)))
            .thenReturn(Optional.of("signed-token"));
        when(approvalTokens.toShortToken("signed-token")).thenReturn(Optional.of("short-id"));

        ActionContextImpl actionContext = createActionContext(approvalTokens);

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(Instant.now());

        assertNotNull(approvalLinks);
        assertEquals("short-id", approvalLinks.shortResumeId());
    }

    @Test
    void testGetApprovalLinksHasNoShortResumeIdWhenTheResumeIdCannotBeShortened() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured(anyString(), any(Duration.class)))
            .thenReturn(Optional.of("signed-token"));
        when(approvalTokens.toShortToken("signed-token")).thenReturn(Optional.empty());

        ActionContextImpl actionContext = createActionContext(approvalTokens);

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(Instant.now());

        assertNotNull(approvalLinks);
        assertNull(approvalLinks.shortResumeId());
    }

    @Test
    void testGetApprovalLinksHasNoShortResumeIdWithoutApprovalTokens() {
        ActionContextImpl actionContext = createActionContext();

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(Instant.now());

        assertNotNull(approvalLinks);
        assertNull(approvalLinks.shortResumeId());
    }

    @Test
    void testGetApprovalLinksIsNullWithoutAPublicUrl() {
        ActionContextImpl actionContext = ActionContextImpl.builder(
            "approval", 1, "requestApproval", false, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class), null)
            .jobId(200L)
            .build();

        assertNull(actionContext.getApprovalLinks(Instant.now()));
    }

    private static ActionContextImpl createActionContext() {
        return createActionContext(null);
    }

    private static ActionContextImpl createActionContext(@Nullable ApprovalTokens approvalTokens) {
        return ActionContextImpl.builder(
            "approval", 1, "requestApproval", false, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class),
            approvalTokens)
            .jobId(200L)
            .publicUrl("https://example.com")
            .build();
    }
}
