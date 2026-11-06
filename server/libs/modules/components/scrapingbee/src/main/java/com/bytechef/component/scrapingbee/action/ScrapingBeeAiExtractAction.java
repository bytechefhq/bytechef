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

package com.bytechef.component.scrapingbee.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_EXTRACT_RULES;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_QUERY;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.AI_SELECTOR;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.EXTRACTION_TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.scrapingbee.util.ScrapingBeeUtils;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeAiExtractAction {

    private static final String QUERY = "QUERY";
    private static final String RULES = "RULES";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("aiExtract")
        .title("AI Extract")
        .description(
            "Scrape a web page and let ScrapingBee's AI extract the requested information, either as an answer to " +
                "a natural-language query or as structured JSON described by extraction rules. Costs 5 credits on " +
                "top of the regular request cost.")
        .properties(
            ScrapingBeeUtils.withRequestProperties(
                string(EXTRACTION_TYPE)
                    .label("Extraction Type")
                    .description("How the information to extract is described.")
                    .options(
                        option("Query", QUERY, "Describe the information to extract in natural language."),
                        option("Rules", RULES, "Describe each output field in a JSON object."))
                    .defaultValue(QUERY)
                    .required(true),
                string(AI_QUERY)
                    .label("Query")
                    .description("The information to extract from the page, e.g. \"price of the product\".")
                    .displayCondition("%s == '%s'".formatted(EXTRACTION_TYPE, QUERY))
                    .required(true),
                string(AI_EXTRACT_RULES)
                    .label("Extract Rules")
                    .description(
                        "JSON object mapping each output key to a description of the data, e.g. " +
                            "{\"price\": \"the product price in dollars\", \"categories\": {\"description\": " +
                            "\"all product categories\", \"type\": \"list\"}}.")
                    .controlType(ControlType.TEXT_AREA)
                    .displayCondition("%s == '%s'".formatted(EXTRACTION_TYPE, RULES))
                    .required(true),
                string(AI_SELECTOR)
                    .label("AI Selector")
                    .description(
                        "CSS selector that limits the AI extraction to part of the page, improving accuracy and " +
                            "speed.")
                    .required(false)))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/scrapingbee_v1#ai-extract")
        .perform(ScrapingBeeAiExtractAction::perform);

    private ScrapingBeeAiExtractAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> queryParameters = ScrapingBeeUtils.requestQueryParameters(inputParameters);

        boolean rules = RULES.equals(inputParameters.getString(EXTRACTION_TYPE, QUERY));

        if (rules) {
            queryParameters.addAll(List.of(AI_EXTRACT_RULES, inputParameters.getRequiredString(AI_EXTRACT_RULES)));
        } else {
            queryParameters.addAll(List.of(AI_QUERY, inputParameters.getRequiredString(AI_QUERY)));
        }

        String aiSelector = inputParameters.getString(AI_SELECTOR);

        if (aiSelector != null) {
            queryParameters.addAll(List.of(AI_SELECTOR, aiSelector));
        }

        String body = context.http(http -> http.get("/"))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(queryParameters.toArray())
            .execute()
            .getBody(new TypeReference<>() {});

        return parseJson(body, context);
    }

    private static Object parseJson(String body, Context context) {
        if (body == null) {
            return null;
        }

        String trimmedBody = body.strip();

        if (trimmedBody.startsWith("{") || trimmedBody.startsWith("[")) {
            return context.json(json -> json.read(trimmedBody));
        }

        return body;
    }
}
