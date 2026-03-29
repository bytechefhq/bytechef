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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.llm.LLMJudge;
import org.springaicommunity.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * An LLM-based judge that evaluates agent output against a user-defined rule.
 *
 * <p>
 * The rule is sent to the LLM along with the conversation transcript, and the LLM determines whether the agent's
 * behavior satisfies the rule.
 * </p>
 */
public class LlmRuleJudge extends LLMJudge {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String rule;

    public LlmRuleJudge(String name, ChatClient.Builder chatClientBuilder, String rule) {
        super(name, "Evaluates against: " + rule, chatClientBuilder);

        this.rule = rule;
    }

    @Override
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    protected String buildPrompt(JudgmentContext context) {
        String transcript = (String) context.metadata()
            .getOrDefault("transcript", "");

        String agentOutput = context.agentOutput()
            .orElse("");

        return """
            You are an evaluation judge. Your task is to determine whether an AI agent's behavior \
            satisfies a specific rule.

            ## Rule
            %s

            ## Conversation Transcript
            %s

            ## Agent Output
            %s

            ## Instructions
            Evaluate whether the agent's behavior satisfies the rule above. \
            Respond with ONLY a JSON object in the following format:
            {"passed": true, "explanation": "Brief explanation of why the rule was or was not satisfied"}

            Do not include any text outside the JSON object.""".formatted(rule, transcript, agentOutput);
    }

    @Override
    protected Judgment parseResponse(String response, JudgmentContext context) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(response.strip());

            JsonNode passedNode = jsonNode.get("passed");
            JsonNode explanationNode = jsonNode.get("explanation");

            if (passedNode == null || explanationNode == null) {
                return Judgment.error(
                    "Failed to parse LLM response",
                    new IllegalArgumentException("Missing 'passed' or 'explanation' field in response"));
            }

            String explanation = explanationNode.asText();

            if (passedNode.asBoolean()) {
                return Judgment.pass(explanation);
            }

            return Judgment.fail(explanation);
        } catch (JacksonException jsonProcessingException) {
            return Judgment.error("Failed to parse LLM response", jsonProcessingException);
        }
    }

}
