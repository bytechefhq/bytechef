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

package com.bytechef.component.html.helper.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.ATTRIBUTE;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.CONTENT;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.EXTRACT_CONTENT;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.QUERY_SELECTOR;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.RETURN_ARRAY;
import static com.bytechef.component.html.helper.constant.HtmlHelperConstants.RETURN_VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OutputSchemaDataSource.ActionOutputSchemaFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.component.definition.SampleOutputDataSource.SampleOutputResponse;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Ivica Cardic
 */
public class HtmlHelperExtractContentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(EXTRACT_CONTENT)
        .title("Extract Content")
        .description("Extract content from the HTML content.")
        .properties(
            string(CONTENT)
                .label("HTML content to extract content from.")
                .description("The HTML content.")
                .controlType(Property.ControlType.TEXT_AREA)
                .required(true),
            string(QUERY_SELECTOR)
                .label("CSS Selector")
                .description("The CSS selector to search for.")
                .required(true),
            string(RETURN_VALUE)
                .label("Return Value")
                .description("The data to return.")
                .options(
                    option("Attribute", "attribute", "Get the attribute value like 'class' from an element."),
                    option("HTML", "html", "Get the HTML content that the element contains."),
                    option("Text", "text", "Get the text content of the element."))
                .required(true)
                .defaultValue("html"),
            string(ATTRIBUTE)
                .label("Attribute")
                .description("The name of the attribute to return the value of")
                .required(true)
                .displayCondition("%s === 'attribute'".formatted(RETURN_VALUE)),
            bool(RETURN_ARRAY)
                .label("Return Array")
                .description(
                    "If selected, then extracted individual items are returned as an array. If you don't set this, all values are returned as a single string."))
        .outputSchema(getOutputSchemaFunction())
        .sampleOutput(getSampleOutputFunction())
        .perform(HtmlHelperExtractContentAction::perform);

    protected static ActionOutputSchemaFunction getOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> context.outputSchema(
            outputSchema -> outputSchema.get(perform(inputParameters, connectionParameters, context)));
    }

    protected static ActionSampleOutputFunction getSampleOutputFunction() {
        return (inputParameters, connectionParameters, context) -> new SampleOutputResponse(
            perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Object result;

        Document document = Jsoup.parse(inputParameters.getRequiredString(CONTENT));

        Elements elements = document.select(inputParameters.getRequiredString(QUERY_SELECTOR));

        Stream<String> items = elements.stream()
            .map(element -> getValue(element, inputParameters));

        if (inputParameters.getBoolean(RETURN_ARRAY, false)) {
            result = items.toList();
        } else {
            result = items.collect(Collectors.joining(" "));
        }

        return result;
    }

    private static String getValue(Element element, Parameters inputParameters) {
        String returnValue = inputParameters.getRequiredString(RETURN_VALUE);

        return switch (returnValue) {
            case "attribute" -> element.attr(inputParameters.getRequiredString(ATTRIBUTE));
            case "html" -> element.html();
            case "text" -> element.text();
            default -> throw new IllegalArgumentException("Unknown return value: %s".formatted(returnValue));
        };
    }
}
