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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * A deterministic judge that checks whether the agent output matches (or does not match) a regular expression pattern.
 */
public class RegexMatchJudge extends DeterministicJudge {

    private final String mode;
    private final String pattern;

    public RegexMatchJudge(String pattern, String mode) {
        super("RegexMatch", "Checks if agent output matches or does not match a regular expression pattern");

        this.pattern = pattern;
        this.mode = mode;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");

        Pattern compiledPattern;

        try {
            compiledPattern = Pattern.compile(pattern);
        } catch (PatternSyntaxException patternSyntaxException) {
            return Judgment.error("Invalid regex pattern: " + pattern, patternSyntaxException);
        }

        Matcher matcher = compiledPattern.matcher(output);
        boolean matches = matcher.find();

        if ("MUST_MATCH".equals(mode)) {
            if (matches) {
                return Judgment.pass("Output matches the required pattern: '" + pattern + "'");
            }

            return Judgment.fail("Output does not match the required pattern: '" + pattern + "'");
        } else {
            if (matches) {
                return Judgment.fail("Output matches the prohibited pattern: '" + pattern + "'");
            }

            return Judgment.pass("Output does not match the prohibited pattern: '" + pattern + "'");
        }
    }

}
