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

package com.bytechef.platform.component.rule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.RiskLevel;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ToolRiskLevelResolverTest {

    @Test
    void testADeclaredLevelWinsOverInference() {
        assertThat(ToolRiskLevelResolver.resolve(RiskLevel.LOW, "deleteRecord")).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void testDestructiveAndMonetaryNamesAreCritical() {
        assertThat(ToolRiskLevelResolver.resolve(null, "deleteRecord")).isEqualTo(RiskLevel.CRITICAL);
        assertThat(ToolRiskLevelResolver.resolve(null, "purge_all")).isEqualTo(RiskLevel.CRITICAL);
        assertThat(ToolRiskLevelResolver.resolve(null, "createRefund")).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void testOutwardFacingNamesAreHigh() {
        assertThat(ToolRiskLevelResolver.resolve(null, "sendMessage")).isEqualTo(RiskLevel.HIGH);
        assertThat(ToolRiskLevelResolver.resolve(null, "publishPost")).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void testReadNamesAreLow() {
        assertThat(ToolRiskLevelResolver.resolve(null, "getRecord")).isEqualTo(RiskLevel.LOW);
        assertThat(ToolRiskLevelResolver.resolve(null, "searchCustomers")).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void testAnUnknownNameIsMedium() {
        assertThat(ToolRiskLevelResolver.resolve(null, "frobnicate")).isEqualTo(RiskLevel.MEDIUM);
        assertThat(ToolRiskLevelResolver.resolve(null, "createRecord")).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void testTheHighestMatchingTokenWins() {
        assertThat(ToolRiskLevelResolver.resolve(null, "deleteAndNotify")).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void testDisablingAndDeactivatingAreHigh() {
        assertThat(ToolRiskLevelResolver.resolve(null, "disableWebhook")).isEqualTo(RiskLevel.HIGH);
        assertThat(ToolRiskLevelResolver.resolve(null, "deactivateAccount")).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void testADigitBetweenLetterCasesStillSplitsTheToken() {
        assertThat(ToolRiskLevelResolver.resolve(null, "deleteV2Something")).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void testADigitGluedOntoALowercaseVerbStillSplitsTheToken() {
        assertThat(ToolRiskLevelResolver.resolve(null, "delete2FactorCode")).isEqualTo(RiskLevel.CRITICAL);
    }
}
