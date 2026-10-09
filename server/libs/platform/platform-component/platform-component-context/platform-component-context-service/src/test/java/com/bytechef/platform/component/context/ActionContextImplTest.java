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
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext.Approval.Links;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
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

    private static ActionContextImpl createActionContext() {
        return ActionContextImpl.builder(
            "approval", 1, "requestApproval", false, mock(CacheManager.class), mock(DataStorage.class),
            mock(ApplicationEventPublisher.class), mock(HttpClientExecutor.class), mock(TempFileStorage.class))
            .jobId(200L)
            .publicUrl("https://example.com")
            .build();
    }
}
