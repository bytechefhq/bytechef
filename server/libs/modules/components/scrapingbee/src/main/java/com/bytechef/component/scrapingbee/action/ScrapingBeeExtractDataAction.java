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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.EXTRACT_RULES;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.scrapingbee.util.ScrapingBeeUtils;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeExtractDataAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractData")
        .title("Extract Data")
        .description("Scrape a web page and extract structured JSON data from it using CSS or XPath selectors.")
        .properties(
            ScrapingBeeUtils.withRequestProperties(
                string(EXTRACT_RULES)
                    .label("Extract Rules")
                    .description(
                        "JSON object mapping each output key to a CSS or XPath selector, e.g. " +
                            "{\"title\": \"h1\", \"links\": {\"selector\": \"a\", \"type\": \"list\", " +
                            "\"output\": \"@href\"}}.")
                    .controlType(ControlType.TEXT_AREA)
                    .required(true)))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/scrapingbee_v1#extract-data")
        .perform(ScrapingBeeExtractDataAction::perform);

    private ScrapingBeeExtractDataAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> queryParameters = ScrapingBeeUtils.requestQueryParameters(inputParameters);

        queryParameters.addAll(List.of(EXTRACT_RULES, inputParameters.getRequiredString(EXTRACT_RULES)));

        return context.http(http -> http.get("/"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(queryParameters.toArray())
            .execute()
            .getBody();
    }
}
