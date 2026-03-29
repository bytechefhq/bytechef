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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.llm.LLMJudge;
import org.springaicommunity.judge.result.Judgment;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * An LLM-based judge that evaluates agent output against a user-defined rule.
 *
 * <p>
 * The rule is sent to the LLM along with the conversation transcript, and the LLM determines whether the agent's
 * behavior satisfies the rule.
 * </p>
 */
public class LlmRuleJudge extends LLMJudge {

    private static final Logger logger = LoggerFactory.getLogger(LlmRuleJudge.class);

    private static final int MAX_TRANSCRIPT_LENGTH = 100_000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String rule;
    private final boolean truncateLongConversations;

    public LlmRuleJudge(String name, ChatClient.Builder chatClientBuilder, String rule,
        boolean truncateLongConversations) {

        super(name, "Evaluates against: " + rule, chatClientBuilder);

        this.rule = rule;
        this.truncateLongConversations = truncateLongConversations;
    }

    @Override
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    protected String buildPrompt(JudgmentContext context) {
        String transcript = (String) context.metadata()
            .getOrDefault("transcript", "");

        if (truncateLongConversations && transcript.length() > MAX_TRANSCRIPT_LENGTH) {
            transcript = truncateTranscript(transcript);
        }

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

    private String truncateTranscript(String transcript) {
        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(transcript);

            JsonNode messagesNode = rootNode.get("messages");

            if (messagesNode == null || !messagesNode.isArray()) {
                return transcript.substring(transcript.length() - MAX_TRANSCRIPT_LENGTH);
            }

            ArrayNode messages = (ArrayNode) messagesNode;

            while (messages.size() > 2) {
                String serialized = OBJECT_MAPPER.writeValueAsString(rootNode);

                if (serialized.length() <= MAX_TRANSCRIPT_LENGTH) {
                    return serialized;
                }

                messages.remove(0);
            }

            String serialized = OBJECT_MAPPER.writeValueAsString(rootNode);

            if (serialized.length() <= MAX_TRANSCRIPT_LENGTH) {
                return serialized;
            }

            return serialized.substring(serialized.length() - MAX_TRANSCRIPT_LENGTH);
        } catch (JacksonException jacksonException) {
            logger.warn("Failed to parse transcript JSON for truncation, falling back to substring", jacksonException);

            return transcript.substring(transcript.length() - MAX_TRANSCRIPT_LENGTH);
        }
    }
}
