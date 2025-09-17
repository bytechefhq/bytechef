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

package com.bytechef.component.zenrows.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.CSS_EXTRACTOR;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.KEY;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.URL;
import static com.bytechef.component.zenrows.constant.ZenRowsConstants.VALUE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZenRowsScrapeUrlWithCssSelectorAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("scrapeUrlWithCssSelectors")
        .title("Scrape URL With CSS Selectors")
        .description("Extracts specific data from a given URL.")
        .properties(
            string(URL)
                .label("URL")
                .description("URL of the site that will be scraped.")
                .required(true),
            array(CSS_EXTRACTOR)
                .label("CSS Extractor")
                .description(
                    "Key-value pairs that will be scraped, where key is arbitrary parameter name and value is CSS " +
                        "element that you want to scrape.")
                .required(true)
                .items(
                    object()
                        .properties(
                            string(KEY)
                                .label("Key")
                                .description("Arbitrary parameter name that will be shown in response JSON.")
                                .required(true),
                            string(VALUE)
                                .label("Value")
                                .description("CSS selector that will be scraped.")
                                .required(true))))
        .output(
            outputSchema(
                string()
                    .description("Scraped data.")))
        .perform(ZenRowsScrapeUrlWithCssSelectorAction::perform);

    private ZenRowsScrapeUrlWithCssSelectorAction() {
    }

    private static Map<String, String> getCssExtractor(List<Object> cssExtractorList) {
        Map<String, String> cssExtractor = new HashMap<>();

        for (Object listElement : cssExtractorList) {
            if (listElement instanceof Map<?, ?> elementMap) {
                cssExtractor.put((String) elementMap.get(KEY), (String) elementMap.get(VALUE));
            }
        }

        return cssExtractor;
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, String> cssExtractor =
            getCssExtractor(inputParameters.getRequiredList(CSS_EXTRACTOR, Object.class));

        return context.http(http -> http.get(""))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(
                URL, inputParameters.getRequiredString(URL),
                CSS_EXTRACTOR, context.json(json -> json.write(cssExtractor)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
