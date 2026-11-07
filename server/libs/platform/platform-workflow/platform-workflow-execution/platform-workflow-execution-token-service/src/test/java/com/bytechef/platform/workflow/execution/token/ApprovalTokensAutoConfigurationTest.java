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

package com.bytechef.platform.workflow.execution.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.EncryptionKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
class ApprovalTokensAutoConfigurationTest {

    private static final String INNER = "dGVuYW50OjQyOnV1aWQtMTIzOnRydWU=";

    @Test
    void testApprovalTokensFallBackToDefaultTtlAndClockSkewWhenUnset() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Approval approval = applicationProperties.getApproval();

        ApplicationProperties.Approval.SignedToken signedToken = approval.getSignedToken();

        signedToken.setSecret(
            EncodingUtils.base64EncodeToString("approval-token-secret-key-0001!!".getBytes(StandardCharsets.UTF_8)));

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("applicationProperties", applicationProperties);

        ApprovalTokens approvalTokens = new ApprovalTokensAutoConfiguration().approvalTokens(
            beanFactory.getBeanProvider(ApplicationProperties.class), beanFactory.getBeanProvider(Clock.class),
            beanFactory.getBeanProvider(EncryptionKey.class));

        String token = approvalTokens.toSignedTokenIfConfigured(INNER)
            .orElseThrow();

        assertThat(approvalTokens.parseSignedToken(token)).contains(INNER);
    }

    @Test
    void testApprovalTokensAreCreatedWithoutAnApplicationPropertiesBean() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        ApprovalTokens approvalTokens = new ApprovalTokensAutoConfiguration().approvalTokens(
            beanFactory.getBeanProvider(ApplicationProperties.class), beanFactory.getBeanProvider(Clock.class),
            beanFactory.getBeanProvider(EncryptionKey.class));

        assertThat(approvalTokens.toSignedTokenIfConfigured(INNER)).isEmpty();
    }
}
