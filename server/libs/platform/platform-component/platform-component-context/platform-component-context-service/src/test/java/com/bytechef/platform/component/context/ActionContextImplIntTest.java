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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Approval.Links;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.log.EditorLogFileStorage;
import com.bytechef.platform.component.log.EditorLogFileStorageImpl;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageImpl;
import com.bytechef.platform.component.log.TriggerLogFileStorage;
import com.bytechef.platform.component.log.TriggerLogFileStorageImpl;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.EditorTempFileStorage;
import com.bytechef.platform.file.storage.EditorTempFileStorageImpl;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.file.storage.TempFileStorageImpl;
import com.bytechef.platform.workflow.execution.ApprovalId;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import io.micrometer.tracing.Tracer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@MockitoBean(types = DataStorage.class)
@SpringJUnitConfig({
    ContextFactoryImpl.class, ActionContextImplIntTest.ActionContextImplTestConfiguration.class
})
class ActionContextImplIntTest {

    private static final String PUBLIC_URL = "https://example.com";
    private static final String TENANT_ID = "000001";

    @Autowired
    private ContextFactory contextFactory;

    @Test
    void testGetSuspendCarriesTheJobResumeIdOfTheIssuedResumeUrl() {
        ActionContextImpl actionContext = createActionContext(null);

        assertNotNull(actionContext.getResumeUrl());

        actionContext.suspend(new Suspend(Map.of("formUrl", "https://example.com/form"), null));

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertEquals(actionContext.getJobResumeId(), continueParameters.get(MetadataConstants.JOB_RESUME_ID));
        assertEquals("https://example.com/form", continueParameters.get("formUrl"));
    }

    @Test
    void testGetSuspendCarriesTheJobResumeIdOfAResumeUrlIssuedAfterTheSuspend() {
        ActionContextImpl actionContext = createActionContext(null);

        actionContext.suspend(new Suspend(Map.of(), null));

        assertNotNull(actionContext.getResumeUrl());

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertEquals(actionContext.getJobResumeId(), continueParameters.get(MetadataConstants.JOB_RESUME_ID));
    }

    @Test
    void testGetSuspendHasNoJobResumeIdWhenNoResumeUrlWasIssued() {
        ActionContextImpl actionContext = createActionContext(null);

        actionContext.suspend(new Suspend(Map.of("formUrl", "https://example.com/form"), null));

        Suspend suspend = actionContext.getSuspend();

        assertNotNull(suspend);

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertFalse(continueParameters.containsKey(MetadataConstants.JOB_RESUME_ID));
    }

    @Test
    void testGetSuspendIsNullWithoutASuspend() {
        ActionContextImpl actionContext = createActionContext(null);

        assertNotNull(actionContext.getResumeUrl());
        assertNull(actionContext.getSuspend());
    }

    @Test
    void testGetResumeUrlUsesTheTenantTheContextWasCreatedIn() {
        ActionContextImpl actionContext = TenantContext.callWithTenantId(TENANT_ID, () -> createActionContext(null));

        CompletableFuture<String> resumeUrlFuture = CompletableFuture.supplyAsync(actionContext::getResumeUrl);

        String resumeUrl = resumeUrlFuture.join();

        assertNotNull(resumeUrl);

        JobResumeId jobResumeId = JobResumeId.parse(resumeUrl.substring(resumeUrl.lastIndexOf('/') + 1));

        assertEquals(TENANT_ID, jobResumeId.getTenantId());
    }

    @Test
    void testApprovalLinksUseTheTenantTheContextWasCreatedIn() {
        ActionContextImpl actionContext = TenantContext.callWithTenantId(TENANT_ID, () -> createActionContext(null));

        CompletableFuture<Links> linksFuture = CompletableFuture.supplyAsync(
            () -> actionContext.approval(approval -> approval.generateLinks()));

        Links links = linksFuture.join();

        String approvalLink = links.approvalLink();

        ApprovalId approvalId = ApprovalId.parse(approvalLink.substring(approvalLink.lastIndexOf('/') + 1));

        assertEquals(TENANT_ID, approvalId.getTenantId());
    }

    @Test
    void testGetTaskExecutionId() {
        ActionContextImpl actionContext = createActionContext(42L);

        assertEquals(42L, actionContext.getTaskExecutionId());
    }

    @Test
    void testGetTaskExecutionIdWhenUnset() {
        ActionContextImpl actionContext = createActionContext(null);

        assertNull(actionContext.getTaskExecutionId());
    }

    private ActionContextImpl createActionContext(@Nullable Long taskExecutionId) {
        ActionContext actionContext = contextFactory.createActionContext(
            "approval", 1, "requestApproval", null, null, 200L, taskExecutionId, null, null, null, null, false);

        return (ActionContextImpl) actionContext;
    }

    @Configuration
    static class ActionContextImplTestConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            ApplicationProperties applicationProperties = new ApplicationProperties();

            applicationProperties.setPublicUrl(PUBLIC_URL);

            return applicationProperties;
        }

        @Bean
        Base64FileStorageService base64FileStorageService() {
            return new Base64FileStorageService();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        EditorLogFileStorage editorLogFileStorage(Base64FileStorageService base64FileStorageService) {
            return new EditorLogFileStorageImpl(base64FileStorageService);
        }

        @Bean
        EditorTempFileStorage editorTempFileStorage(Base64FileStorageService base64FileStorageService) {
            return new EditorTempFileStorageImpl(base64FileStorageService);
        }

        @Bean
        LogFileStorage logFileStorage(Base64FileStorageService base64FileStorageService) {
            return new LogFileStorageImpl(base64FileStorageService);
        }

        @Bean
        TempFileStorage tempFileStorage(Base64FileStorageService base64FileStorageService) {
            return new TempFileStorageImpl(base64FileStorageService);
        }

        @Bean
        Tracer tracer() {
            return Tracer.NOOP;
        }

        @Bean
        TriggerLogFileStorage triggerLogFileStorage(Base64FileStorageService base64FileStorageService) {
            return new TriggerLogFileStorageImpl(base64FileStorageService);
        }
    }
}
