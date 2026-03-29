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

package com.bytechef.ai.agent.eval.judge;

import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * A deterministic judge that checks whether the agent output exactly matches a specified expected value.
 *
 * @author Ivica Cardic
 */
class StringEqualsJudge extends DeterministicJudge {

    private final boolean caseSensitive;
    private final String expectedValue;

    StringEqualsJudge(String expectedValue, boolean caseSensitive) {
        super("StringEquals", "Checks exact match against: " + expectedValue);

        this.expectedValue = expectedValue;
        this.caseSensitive = caseSensitive;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");

        boolean matches;

        if (caseSensitive) {
            matches = output.equals(expectedValue);
        } else {
            matches = output.equalsIgnoreCase(expectedValue);
        }

        if (matches) {
            return Judgment.pass("Output exactly matches the expected value: '" + expectedValue + "'");
        }

        return Judgment.fail(
            "Output '" + output + "' does not exactly match the expected value: '" + expectedValue + "'");
    }
}
