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
 * A deterministic judge that checks whether the agent output contains (or does not contain) a specified text.
 */
public class ContainsTextJudge extends DeterministicJudge {

    private final String mode;
    private final String text;

    public ContainsTextJudge(String text, String mode) {
        super("ContainsText", "Checks if agent output contains or does not contain the specified text");

        this.text = text;
        this.mode = mode;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");
        boolean containsText = output.toLowerCase()
            .contains(text.toLowerCase());

        if ("MUST_CONTAIN".equals(mode)) {
            if (containsText) {
                return Judgment.pass("Output contains the required text: '" + text + "'");
            }

            return Judgment.fail("Output does not contain the required text: '" + text + "'");
        } else {
            if (containsText) {
                return Judgment.fail("Output contains the prohibited text: '" + text + "'");
            }

            return Judgment.pass("Output does not contain the prohibited text: '" + text + "'");
        }
    }

}
