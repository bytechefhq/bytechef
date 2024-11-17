/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.text.analysis.action;

import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.FORMAT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.PROMPT;
import static com.bytechef.component.ai.text.analysis.constant.AiTextAnalysisConstants.TEXT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Chat;


/**
 * @author Marko Kriskovic
 */
public class SummarizeTextAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("summarizeText")
        .title("Summarize Text")
        .description("AI reads, analyzes and summarizes your text into a shorter format.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text that is to be summarized.")
                .minLength(100),
            integer(FORMAT)
                .label("Format")
                .description("In what format do you wish the text summarized?")
                .options(
                    option("A structured summary with sections", 0),
                    option("A brief title summarizing the content in 4-7 words", 1),
                    option("A single, concise sentence", 2),
                    option("A bulleted list recap", 3),
                    option("Custom Prompt", 4)),
            string(PROMPT)
                .label("Custom Prompt")
                .description("Write your prompt for summarizing text.")
                .displayCondition("format == 4"),
            MAX_TOKENS_PROPERTY,
            TEMPERATURE_PROPERTY)
        .output()
        .perform(SummarizeTextAction::perform);

    private SummarizeTextAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {


        return null;
    }

}
