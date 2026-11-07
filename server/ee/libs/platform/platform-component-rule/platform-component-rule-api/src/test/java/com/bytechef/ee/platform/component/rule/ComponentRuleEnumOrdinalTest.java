/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ComponentRuleEnumOrdinalTest {

    @Test
    void testRulePhaseOrdinalsAreStable() {
        assertThat(RulePhase.BEFORE.ordinal()).isEqualTo(0);
        assertThat(RulePhase.AFTER.ordinal()).isEqualTo(1);
        assertThat(RulePhase.values()).hasSize(2);
    }

    @Test
    void testRuleActionOrdinalsAreStable() {
        assertThat(RuleAction.BLOCK.ordinal()).isEqualTo(0);
        assertThat(RuleAction.TAG.ordinal()).isEqualTo(1);
        assertThat(RuleAction.REQUIRE_APPROVAL.ordinal()).isEqualTo(2);
        assertThat(RuleAction.values()).hasSize(3);
    }

    @Test
    void testPhaseAndRuleActionRoundTripThroughOrdinalColumns() {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setPhase(RulePhase.AFTER);
        componentRule.setRuleAction(RuleAction.TAG);

        assertThat(componentRule.getPhase()).isEqualTo(RulePhase.AFTER);
        assertThat(componentRule.getRuleAction()).isEqualTo(RuleAction.TAG);
    }
}
