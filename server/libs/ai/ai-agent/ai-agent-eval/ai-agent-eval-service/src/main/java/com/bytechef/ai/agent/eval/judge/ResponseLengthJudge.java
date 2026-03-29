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
 * A deterministic judge that checks whether the agent output length is within the specified min/max character bounds.
 */
public class ResponseLengthJudge extends DeterministicJudge {

    private final Integer maxLength;
    private final Integer minLength;

    public ResponseLengthJudge(Integer maxLength, Integer minLength) {
        super("ResponseLength", "Checks if agent output length is within the specified min/max character bounds");

        this.maxLength = maxLength;
        this.minLength = minLength;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");
        int length = output.length();

        if ((minLength != null) && (length < minLength)) {
            return Judgment.fail(
                "Output length " + length + " is below the minimum required length of " + minLength);
        }

        if ((maxLength != null) && (length > maxLength)) {
            return Judgment.fail(
                "Output length " + length + " exceeds the maximum allowed length of " + maxLength);
        }

        return Judgment.pass("Output length " + length + " is within the acceptable range");
    }

}
