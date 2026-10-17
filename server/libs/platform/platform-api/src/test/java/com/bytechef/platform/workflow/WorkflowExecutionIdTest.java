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

package com.bytechef.platform.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionIdTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testParseRoundTripsTheDefaultTenant() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 7L, "c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e", "trigger_1");

        WorkflowExecutionId parsedWorkflowExecutionId = WorkflowExecutionId.parse(workflowExecutionId.toString());

        assertThat(parsedWorkflowExecutionId.getTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        assertThat(parsedWorkflowExecutionId.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(parsedWorkflowExecutionId.getJobPrincipalId()).isEqualTo(7L);
        assertThat(parsedWorkflowExecutionId.getWorkflowUuid()).isEqualTo("c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e");
        assertThat(parsedWorkflowExecutionId.getTriggerName()).isEqualTo("trigger_1");
    }

    @Test
    void testParseRoundTripsANumberedTenant() {
        TenantContext.setCurrentTenantId("000003");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, 9L, "c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e", "trigger_1");

        WorkflowExecutionId parsedWorkflowExecutionId = WorkflowExecutionId.parse(workflowExecutionId.toString());

        assertThat(parsedWorkflowExecutionId.getTenantId()).isEqualTo("000003");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a b", "x;y", "bad'id", "a\"b", ""
    })
    void testParseRejectsAnInvalidTenantId(String tenantId) {
        String id = encode(tenantId + ":0:1:c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e:trigger_1");

        assertThatThrownBy(() -> WorkflowExecutionId.parse(id))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "public:0:1:uuid", "public:0:1:uuid:trigger_1:extra", "public:99:1:uuid:trigger_1",
        "public:-1:1:uuid:trigger_1", "public:x:1:uuid:trigger_1", "public:0:x:uuid:trigger_1"
    })
    void testParseRejectsAMalformedId(String decodedId) {
        String id = encode(decodedId);

        assertThatThrownBy(() -> WorkflowExecutionId.parse(id))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "not base64!"
    })
    void testParseRejectsAnUndecodableId(String id) {
        assertThatThrownBy(() -> WorkflowExecutionId.parse(id))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static String encode(String value) {
        return EncodingUtils.base64EncodeToString(value);
    }
}
