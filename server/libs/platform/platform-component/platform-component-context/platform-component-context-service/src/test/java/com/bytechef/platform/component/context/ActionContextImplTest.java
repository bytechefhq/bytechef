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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext.Approval.Links;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware.ApprovalLinks;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.ApprovalShortTokenService;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ActionContextImplTest {

    private static final String TENANT_ID = "000001";

    @Test
    void testGetSuspendCarriesTheJobResumeIdOfTheIssuedResumeUrl() {
        ActionContextImpl actionContext = createActionContext();

        assertNotNull(actionContext.getResumeUrl());

        actionContext.suspend(new Suspend(Map.of("formUrl", "https://example.com/form"), null));

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);
        assertEquals(
            actionContext.getJobResumeId(), suspend.continueParameters()
                .get(MetadataConstants.JOB_RESUME_ID));
        assertEquals(
            "https://example.com/form", suspend.continueParameters()
                .get("formUrl"));
    }

    @Test
    void testGetSuspendCarriesTheJobResumeIdOfAResumeUrlIssuedAfterTheSuspend() {
        ActionContextImpl actionContext = createActionContext();

        actionContext.suspend(new Suspend(Map.of(), null));

        assertNotNull(actionContext.getResumeUrl());

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);
        assertEquals(
            actionContext.getJobResumeId(), suspend.continueParameters()
                .get(MetadataConstants.JOB_RESUME_ID));
    }

    @Test
    void testGetSuspendHasNoJobResumeIdWhenNoResumeUrlWasIssued() {
        ActionContextImpl actionContext = createActionContext();

        actionContext.suspend(new Suspend(Map.of("formUrl", "https://example.com/form"), null));

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);
        assertFalse(suspend.continueParameters()
            .containsKey(MetadataConstants.JOB_RESUME_ID));
    }

    @Test
    void testGetSuspendIsNullWithoutASuspend() {
        ActionContextImpl actionContext = createActionContext();

        assertNotNull(actionContext.getResumeUrl());
        assertNull(actionContext.getSuspend());
    }

    @Test
    void testGetResumeUrlUsesTheTenantTheContextWasCreatedIn() {
        ActionContextImpl actionContext = TenantContext.callWithTenantId(TENANT_ID, () -> createActionContext());

        String resumeUrl = CompletableFuture.supplyAsync(actionContext::getResumeUrl)
            .join();

        assertNotNull(resumeUrl);

        JobResumeId jobResumeId = JobResumeId.parse(resumeUrl.substring(resumeUrl.lastIndexOf('/') + 1));

        assertEquals(TENANT_ID, jobResumeId.getTenantId());
    }

    @Test
    void testApprovalLinksUseTheTenantTheContextWasCreatedIn() {
        ActionContextImpl actionContext = TenantContext.callWithTenantId(TENANT_ID, () -> createActionContext());

        Links links = CompletableFuture.supplyAsync(() -> actionContext.approval(approval -> approval.generateLinks()))
            .join();

        String approvalLink = links.approvalLink();

        ApprovalId approvalId = ApprovalId.parse(approvalLink.substring(approvalLink.lastIndexOf('/') + 1));

        assertEquals(TENANT_ID, approvalId.getTenantId());
    }

    @Test
    void testGetApprovalLinksSignsTheTokenUntilTheApprovalExpires() {
        ApprovalTokens approvalTokens = mock(ApprovalTokens.class);

        when(approvalTokens.toSignedTokenIfConfigured(anyString(), any(Duration.class)))
            .thenReturn(Optional.of("signed-token"));

        ActionContextImpl actionContext = createActionContext(approvalTokens);

        Instant expiresAt = Instant.now()
            .plus(Duration.ofDays(45));

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
    void testGetApprovalLinksMintsAShortResumeIdThatOutlivesTheApproval() {
        ApprovalShortTokenService approvalShortTokenService = mock(ApprovalShortTokenService.class);

        when(approvalShortTokenService.mint(anyString(), any(Instant.class))).thenReturn("short-id");

        ActionContextImpl actionContext = createActionContext(null, approvalShortTokenService);

        Instant expiresAt = Instant.now()
            .plus(Duration.ofDays(2));

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(expiresAt);

        assertNotNull(approvalLinks);
        assertEquals("short-id", approvalLinks.shortResumeId());

        ArgumentCaptor<Instant> expiryDateArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(approvalShortTokenService).mint(eq(approvalLinks.resumeId()), expiryDateArgumentCaptor.capture());

        assertEquals(expiresAt.plus(ApprovalFormUrls.APPROVAL_TOKEN_TTL_MARGIN), expiryDateArgumentCaptor.getValue());
    }

    @Test
    void testGetApprovalLinksFallsBackToNoShortResumeIdWhenMintingFails() {
        ApprovalShortTokenService approvalShortTokenService = mock(ApprovalShortTokenService.class);

        when(approvalShortTokenService.mint(anyString(), any(Instant.class)))
            .thenThrow(new UnsupportedOperationException());

        ActionContextImpl actionContext = createActionContext(null, approvalShortTokenService);

        ApprovalLinks approvalLinks = actionContext.getApprovalLinks(Instant.now());

        assertNotNull(approvalLinks);
        assertNull(approvalLinks.shortResumeId());
    }

    @Test
    void testGetApprovalLinksHasNoShortResumeIdWithoutAShortTokenService() {
        ApprovalLinks approvalLinks = createActionContext().getApprovalLinks(Instant.now());

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
        return createActionContext(approvalTokens, null);
    }

    private static ActionContextImpl createActionContext(
        @Nullable ApprovalTokens approvalTokens, @Nullable ApprovalShortTokenService approvalShortTokenService) {

        return ActionContextImpl.builder(
            "approval", 1, "requestApproval", false, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class),
            approvalTokens)
            .approvalShortTokenService(approvalShortTokenService)
            .jobId(200L)
            .publicUrl("https://example.com")
            .build();
    }
}
