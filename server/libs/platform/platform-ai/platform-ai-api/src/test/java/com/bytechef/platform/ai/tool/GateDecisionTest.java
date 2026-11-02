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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class GateDecisionTest {

    @Test
    void testApprovedDecisionCarriesReviewerAndComment() {
        GateDecision gateDecision = GateDecision.from(
            Map.of("approved", true, "approvedBy", "ana@example.com", "comment", "ship it"));

        assertThat(gateDecision).isEqualTo(new GateDecision.Approved("ana@example.com", "ship it"));
    }

    @Test
    void testDeniedDecisionReadsTheReviewerFromApprovedBy() {
        GateDecision gateDecision = GateDecision.from(Map.of("approved", false, "approvedBy", "ana@example.com"));

        assertThat(gateDecision).isEqualTo(new GateDecision.Denied("ana@example.com", null));
    }

    @Test
    void testAbsentApprovedDefaultsToDeny() {
        assertThat(GateDecision.from(Map.of("comment", "hmm"))).isEqualTo(new GateDecision.Denied(null, "hmm"));
    }

    @Test
    void testStringApprovedIsAccepted() {
        assertThat(GateDecision.from(Map.of("approved", "true"))).isInstanceOf(GateDecision.Approved.class);
        assertThat(GateDecision.from(Map.of("approved", "nope"))).isInstanceOf(GateDecision.Denied.class);
    }

    @Test
    void testBlankReviewerAndCommentAreAbsent() {
        Map<String, Object> resumeData = new HashMap<>();

        resumeData.put("approved", true);
        resumeData.put("approvedBy", " ");
        resumeData.put("comment", "");

        assertThat(GateDecision.from(resumeData)).isEqualTo(new GateDecision.Approved(null, null));
    }
}
