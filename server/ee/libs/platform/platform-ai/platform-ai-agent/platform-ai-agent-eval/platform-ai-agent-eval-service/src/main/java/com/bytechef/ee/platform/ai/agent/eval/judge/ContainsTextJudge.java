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
 * A deterministic judge that checks whether the agent output contains (or does not contain) a specified text.
 */
public class ContainsTextJudge extends DeterministicJudge<CompletionEvidence> {

    private final String mode;
    private final String text;

    public ContainsTextJudge(Supplier<CompletionEvidence> evidence, String text, String mode) {
        super(evidence, "ContainsText", "Checks if agent output contains or does not contain the specified text");

        this.text = text;
        this.mode = mode;
    }

    @Override
    protected Judgment evaluate(CompletionEvidence evidence) {
        String output = Objects.requireNonNullElse(evidence.response(), "");
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
