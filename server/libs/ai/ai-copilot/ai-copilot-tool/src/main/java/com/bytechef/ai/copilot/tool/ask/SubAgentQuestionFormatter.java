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

package com.bytechef.ai.copilot.tool.ask;

import com.bytechef.commons.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
public final class SubAgentQuestionFormatter {

    private SubAgentQuestionFormatter() {
    }

    public static String format(String toolName, String pendingQuestionJson) {
        JsonNode envelopeJsonNode;

        try {
            envelopeJsonNode = JsonUtils.readTree(pendingQuestionJson);
        } catch (JacksonException exception) {
            return pendingQuestionJson;
        }

        JsonNode questionsJsonNode = envelopeJsonNode.get("questions");

        if (questionsJsonNode == null || !questionsJsonNode.isArray() || questionsJsonNode.size() == 0) {
            return pendingQuestionJson;
        }

        List<String> questionBlocks = new ArrayList<>();

        for (JsonNode questionJsonNode : questionsJsonNode) {
            questionBlocks.add(formatQuestion(questionJsonNode));
        }

        return "The " + toolName + " agent needs a decision before continuing:\n\n" +
            String.join("\n\n", questionBlocks) +
            "\n\nPresent these options to the user. Then call " + toolName +
            " again, restating the original request together with the chosen answer — this agent does not carry" +
            " anything over from the call that asked, so an answer on its own is not enough to act on.";
    }

    private static String formatQuestion(JsonNode questionJsonNode) {
        String questionText = textOrDefault(questionJsonNode.get("question"), "(no question text provided)");

        JsonNode optionsJsonNode = questionJsonNode.get("options");

        List<String> optionLines = new ArrayList<>();

        if (optionsJsonNode != null && optionsJsonNode.isArray()) {
            int index = 1;

            for (JsonNode optionJsonNode : optionsJsonNode) {
                optionLines.add(index + ". " + formatOption(optionJsonNode));

                index++;
            }
        }

        if (optionLines.isEmpty()) {
            optionLines.add("(no options provided)");
        }

        return questionText + "\n\n" + String.join("\n", optionLines);
    }

    private static String formatOption(JsonNode optionJsonNode) {
        String label = textOrDefault(optionJsonNode.get("label"), "(unlabeled option)");
        String description = textOrDefault(optionJsonNode.get("description"), null);

        return description == null ? label : label + " — " + description;
    }

    private static @Nullable String textOrDefault(@Nullable JsonNode jsonNode, @Nullable String defaultValue) {
        if (jsonNode == null || jsonNode.isNull()) {
            return defaultValue;
        }

        String text = jsonNode.asString();

        return text.isBlank() ? defaultValue : text;
    }
}
