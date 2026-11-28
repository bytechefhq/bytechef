/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalEnumOrdinalTest {

    @Test
    void testStatusOrdinalsAreStable() {
        assertThat(AiHubToolApproval.Status.PENDING.ordinal()).isEqualTo(0);
        assertThat(AiHubToolApproval.Status.APPROVED.ordinal()).isEqualTo(1);
        assertThat(AiHubToolApproval.Status.REJECTED.ordinal()).isEqualTo(2);
        assertThat(AiHubToolApproval.Status.EXPIRED.ordinal()).isEqualTo(3);
        assertThat(AiHubToolApproval.Status.SUPERSEDED.ordinal()).isEqualTo(4);
        assertThat(AiHubToolApproval.Status.FAILED.ordinal()).isEqualTo(5);
        assertThat(AiHubToolApproval.Status.values()).hasSize(6);
    }

    @Test
    void testRuleEnumOrdinalsAreStable() {
        assertThat(AiHubToolApproval.ToolKind.CATALOG.ordinal()).isEqualTo(0);
        assertThat(AiHubToolApproval.ToolKind.COMPONENT.ordinal()).isEqualTo(1);
        assertThat(AiHubToolApprovalRule.Mode.REQUIRE.ordinal()).isEqualTo(0);
        assertThat(AiHubToolApprovalRule.Mode.EXEMPT.ordinal()).isEqualTo(1);
    }

    @Test
    void testStatusRoundTripsThroughTheOrdinalColumn() {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setStatus(AiHubToolApproval.Status.SUPERSEDED);

        assertThat(approval.getStatus()).isEqualTo(AiHubToolApproval.Status.SUPERSEDED);
    }
}
