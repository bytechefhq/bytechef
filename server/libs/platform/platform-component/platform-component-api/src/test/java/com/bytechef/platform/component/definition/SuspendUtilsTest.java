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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext.Suspend;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendUtilsTest {

    private static final Instant INSTANT = Instant.parse("2026-10-16T10:15:30Z");

    @Test
    void testExtractSuspendExpiresAtReadsAnIsoString() {
        assertThat(SuspendUtils.extractSuspendExpiresAt(Map.of("suspend", Map.of("expiresAt", INSTANT.toString()))))
            .isEqualTo(INSTANT);
    }

    @Test
    void testExtractSuspendExpiresAtReadsEpochMilliseconds() {
        assertThat(
            SuspendUtils.extractSuspendExpiresAt(Map.of("suspend", Map.of("expiresAt", INSTANT.toEpochMilli()))))
                .isEqualTo(INSTANT);
    }

    @Test
    void testExtractSuspendExpiresAtIgnoresAnUnparseableValue() {
        assertThat(SuspendUtils.extractSuspendExpiresAt(Map.of("suspend", Map.of("expiresAt", "tomorrow")))).isNull();
        assertThat(SuspendUtils.extractSuspendExpiresAt(Map.of())).isNull();
    }

    @Test
    void testExtractSuspendExpiresAtReadsASuspendInstance() {
        Suspend suspend = new Suspend(Map.of("approvalRaisedAt", INSTANT.toString()), INSTANT.plusSeconds(60));

        Map<String, ?> metadata = Map.of("suspend", suspend);

        assertThat(SuspendUtils.extractSuspendExpiresAt(metadata)).isEqualTo(INSTANT.plusSeconds(60));
        assertThat(SuspendUtils.extractApprovalRaisedAt(metadata)).isEqualTo(INSTANT);
    }

    @Test
    void testExtractApprovalRaisedAtReadsTheContinueParameters() {
        Map<String, ?> metadata = Map.of(
            "suspend", Map.of("continueParameters", Map.of("approvalRaisedAt", INSTANT.toString())));

        assertThat(SuspendUtils.extractApprovalRaisedAt(metadata)).isEqualTo(INSTANT);
        assertThat(SuspendUtils.extractApprovalRaisedAt(Map.of("suspend", Map.of()))).isNull();
    }
}
