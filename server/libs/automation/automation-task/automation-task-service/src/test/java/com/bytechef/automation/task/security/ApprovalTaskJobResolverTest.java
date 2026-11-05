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

package com.bytechef.automation.task.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskJobResolverTest {

    private static final long JOB_ID = 9L;
    private static final String JOB_RESUME_ID = JobResumeId.of("public", JOB_ID)
        .toString();

    private ApprovalTokens approvalTokens;
    private ApprovalTaskJobResolver approvalTaskJobResolver;

    @BeforeEach
    void beforeEach() {
        approvalTokens = approvalTokens("approval-token-secret-key-0001!!", false);
        approvalTaskJobResolver = approvalTaskJobResolver(approvalTokens);
    }

    @Test
    void testGetJobIdResolvesTheJobOfASignedApprovalToken() {
        String signedToken = approvalTokens.toSignedToken(JOB_RESUME_ID, Duration.ofDays(1));

        assertThat(approvalTaskJobResolver.getJobId(signedToken)).isEqualTo(JOB_ID);
    }

    @Test
    void testGetJobIdRefusesAnApprovalTokenSignedWithAnotherSecret() {
        ApprovalTokens otherApprovalTokens = approvalTokens("approval-token-secret-key-0002!!", false);

        String forgedToken = otherApprovalTokens.toSignedToken(JOB_RESUME_ID, Duration.ofDays(1));

        assertThat(approvalTaskJobResolver.getJobId(forgedToken)).isEqualTo(-1L);
    }

    @Test
    void testGetJobIdRefusesAnExpiredApprovalToken() {
        String expiredToken = approvalTokens.toSignedToken(JOB_RESUME_ID, Duration.ofDays(-1));

        assertThat(approvalTaskJobResolver.getJobId(expiredToken)).isEqualTo(-1L);
    }

    @Test
    void testGetJobIdResolvesAnUnsignedJobResumeIdWhileSignedTokensAreOptional() {
        assertThat(approvalTaskJobResolver.getJobId(JOB_RESUME_ID)).isEqualTo(JOB_ID);
    }

    @Test
    void testGetJobIdRefusesAnUnsignedJobResumeIdWhenSignedTokensAreRequired() {
        ApprovalTaskJobResolver requiringApprovalTaskJobResolver = approvalTaskJobResolver(
            approvalTokens("approval-token-secret-key-0001!!", true));

        assertThat(requiringApprovalTaskJobResolver.getJobId(JOB_RESUME_ID)).isEqualTo(-1L);
    }

    @Test
    void testGetJobIdResolvesAJobResumeIdWithoutApprovalTokens() {
        ApprovalTaskJobResolver tokenlessApprovalTaskJobResolver = new ApprovalTaskJobResolver(
            new StaticListableBeanFactory().getBeanProvider(ApprovalTokens.class));

        assertThat(tokenlessApprovalTaskJobResolver.getJobId(JOB_RESUME_ID)).isEqualTo(JOB_ID);
    }

    @Test
    void testGetJobIdRefusesAnAbsentOrUnparsableJobResumeId() {
        assertThat(approvalTaskJobResolver.getJobId(null)).isEqualTo(-1L);
        assertThat(approvalTaskJobResolver.getJobId("not-a-job-resume-id")).isEqualTo(-1L);
    }

    private static ApprovalTokens approvalTokens(String secret, boolean signedTokenRequired) {
        Base64.Encoder encoder = Base64.getEncoder();

        return new ApprovalTokensImpl(
            Clock.systemUTC(), encoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8)), List.of(),
            Duration.ofDays(1), Duration.ofSeconds(60), signedTokenRequired);
    }

    private static ApprovalTaskJobResolver approvalTaskJobResolver(ApprovalTokens approvalTokens) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of("approvalTokens", approvalTokens));

        return new ApprovalTaskJobResolver(beanFactory.getBeanProvider(ApprovalTokens.class));
    }
}
