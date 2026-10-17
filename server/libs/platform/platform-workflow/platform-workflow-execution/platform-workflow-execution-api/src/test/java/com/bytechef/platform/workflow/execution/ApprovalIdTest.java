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

package com.bytechef.platform.workflow.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class ApprovalIdTest {

    @Test
    void testParseReadsAValidId() {
        ApprovalId approvalId = ApprovalId.parse(
            EncodingUtils.base64EncodeToString("000002:42:c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e:true"));

        assertThat(approvalId.getTenantId()).isEqualTo("000002");
        assertThat(approvalId.getJobId()).isEqualTo(42L);
        assertThat(approvalId.getUuidAsString()).isEqualTo("c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e");
        assertThat(approvalId.isApproved()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "a b", "x;y", "bad'id"
    })
    void testParseRejectsAnInvalidTenantId(String tenantId) {
        String id = EncodingUtils.base64EncodeToString(tenantId + ":42:c6a8b8d4-6f7e-4b0a-9a7b-1f0f2b3c4d5e:true");

        assertThatThrownBy(() -> ApprovalId.parse(id))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "000002:42:uuid", "000002:42:uuid:true:extra"
    })
    void testParseRejectsAMalformedId(String decodedId) {
        String id = EncodingUtils.base64EncodeToString(decodedId);

        assertThatThrownBy(() -> ApprovalId.parse(id))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
