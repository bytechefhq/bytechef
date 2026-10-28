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

package com.bytechef.ee.platform.ai.agent.eval.judge;

import io.github.markpollack.judge.DeterministicJudge;
import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A deterministic judge that checks whether the agent output exactly matches a specified expected value.
 *
 * @author Ivica Cardic
 */
final class StringEqualsJudge extends DeterministicJudge<CompletionEvidence> {

    private final boolean caseSensitive;
    private final String expectedValue;

    StringEqualsJudge(Supplier<CompletionEvidence> evidence, String expectedValue, boolean caseSensitive) {
        super(evidence, "StringEquals", "Checks exact match against: " + expectedValue);

        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue");
        this.caseSensitive = caseSensitive;
    }

    @Override
    protected Judgment evaluate(CompletionEvidence evidence) {
        String output = Objects.requireNonNullElse(evidence.response(), "");

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
