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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.CONTENT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.QUERY_SELECTOR;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.RETURN_ARRAY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.RETURN_VALUE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.text.helper.constant.TextHelperConstants;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author Ivica Cardic
 */
public class TextHelperExtractContentFromHtmlAction {

    protected enum ReturnValue {

        ATTRIBUTE, HTML, TEXT;
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractContentFromHtml")
        .title("Extract Content from HTML")
        .description("Extract content from the HTML content.")
        .properties(
            string(CONTENT)
                .label("HTML Content")
                .description("The full HTML document or fragment to extract data from.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(QUERY_SELECTOR)
                .label("CSS Selector")
                .description(
                    "A CSS selector used to locate the element(s) you want to extract (for example: div.article, " +
                        "a[href], #title).")
                .required(true),
            string(RETURN_VALUE)
                .label("Return Value")
                .description("Specifies what content should be extracted from the matched element(s).")
                .options(
                    option(
                        "Attribute", ReturnValue.ATTRIBUTE.name(),
                        "Get the attribute value like 'class' from an element."),
                    option("HTML", ReturnValue.HTML.name(), "Get the HTML content that the element contains."),
                    option("Text", ReturnValue.TEXT.name(), "Get the text content of the element."))
                .required(true)
                .defaultValue("html"),
            string(TextHelperConstants.ATTRIBUTE)
                .label("Attribute")
                .description(
                    "The name of the HTML attribute to extract from the matched element(s) (for example: href, src, " +
                        "or class).")
                .required(true)
                .displayCondition("%s == '%s'".formatted(RETURN_VALUE, ReturnValue.ATTRIBUTE.name())),
            bool(RETURN_ARRAY)
                .label("Return Array")
                .description(
                    "If selected, then extracted individual items are returned as an array. If you don't set this, " +
                        "all values are returned as a single string."))
        .output()
        .help(
            "",
            "https://docs.bytechef.io/reference/components/text-helper_v1#extract-content-from-html")
        .perform(TextHelperExtractContentFromHtmlAction::perform);

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

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
        ReturnValue returnValue = inputParameters.getRequired(RETURN_VALUE, ReturnValue.class);

        return switch (returnValue) {
            case ATTRIBUTE -> element.attr(inputParameters.getRequiredString(TextHelperConstants.ATTRIBUTE));
            case HTML -> element.html();
            case TEXT -> element.text();
        };
    }
}
