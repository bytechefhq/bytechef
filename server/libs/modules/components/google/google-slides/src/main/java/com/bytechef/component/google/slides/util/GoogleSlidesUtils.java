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

package com.bytechef.component.google.slides.util;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.PLACEHOLDER_FORMAT;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Monika Kušter
 */
public class GoogleSlidesUtils {

    private GoogleSlidesUtils() {
    }

    public static List<ModifiableValueProperty<?, ?>> createPropertiesForPlaceholderVariables(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Context context) {

        Map<String, ?> body = getSlide(context, inputParameters.getRequiredString(FILE_ID));
        String placeholderFormat = inputParameters.getRequiredString(PLACEHOLDER_FORMAT);

        return collectPropertiesFromSlides(body, placeholderFormat);
    }

    private static Map<String, ?> getSlide(Context context, String presentationId) {
        return context.http(http -> http.get("/presentations/%s".formatted(presentationId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static List<ModifiableValueProperty<?, ?>> collectPropertiesFromSlides(
        Map<String, ?> body, String placeholderFormat) {

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        if (body.get("slides") instanceof List<?> slides) {
            for (Object slide : slides) {
                if (slide instanceof Map<?, ?> slideMap && slideMap.get("pageElements") instanceof List<?> elements) {
                    properties.addAll(collectPropertiesFromElements(elements, placeholderFormat));
                }
            }
        }

        return properties;
    }

    private static List<ModifiableValueProperty<?, ?>> collectPropertiesFromElements(
        List<?> elements, String placeholderFormat) {

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        for (Object element : elements) {
            if (element instanceof Map<?, ?> elementMap) {
                collectFromShape(elementMap, placeholderFormat, properties);
                collectFromTable(elementMap, placeholderFormat, properties);
            }
        }

        return properties;
    }

    private static void collectFromShape(
        Map<?, ?> elementMap, String placeholderFormat, List<ModifiableValueProperty<?, ?>> properties) {

        if (elementMap.get("shape") instanceof Map<?, ?> shapeMap &&
            shapeMap.get("text") instanceof Map<?, ?> textMap &&
            textMap.get("textElements") instanceof List<?> textElements) {

            addPropertiesFromTextElements(textElements, placeholderFormat, properties);
        }
    }

    private static void collectFromTable(
        Map<?, ?> elementMap, String placeholderFormat, List<ModifiableValueProperty<?, ?>> properties) {

        if (elementMap.get("table") instanceof Map<?, ?> tableMap &&
            tableMap.get("tableRows") instanceof List<?> tableRows) {

            for (Object tableRow : tableRows) {
                if (tableRow instanceof Map<?, ?> tableRowMap &&
                    tableRowMap.get("tableCells") instanceof List<?> tableCells) {

                    for (Object tableCell : tableCells) {
                        if (tableCell instanceof Map<?, ?> tableCellMap &&
                            tableCellMap.get("text") instanceof Map<?, ?> textMap &&
                            textMap.get("textElements") instanceof List<?> textElements) {

                            addPropertiesFromTextElements(textElements, placeholderFormat, properties);
                        }
                    }
                }
            }
        }
    }

    private static void addPropertiesFromTextElements(
        List<?> textElements, String placeholderFormat, List<ModifiableValueProperty<?, ?>> properties) {

        for (Object textElement : textElements) {
            if (textElement instanceof Map<?, ?> textElementMap &&
                textElementMap.get("textRun") instanceof Map<?, ?> textRunMap) {

                String content = (String) textRunMap.get("content");

                if (content != null) {
                    properties.addAll(extractPlaceholders(content, placeholderFormat));
                }
            }
        }
    }

    private static List<ModifiableValueProperty<?, ?>> extractPlaceholders(String content, String placeholderFormat) {
        Pattern pattern = "[[]]".equals(placeholderFormat)
            ? Pattern.compile("\\[\\[([^]]+)]]")
            : Pattern.compile("\\{\\{([^}]+)}}");

        Matcher matcher = pattern.matcher(content);

        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        while (matcher.find()) {
            String matchValue = matcher.group(1)
                .trim();
            String name = "[[]]".equals(placeholderFormat)
                ? "[[" + matchValue + "]]"
                : "{{" + matchValue + "}}";

            String description = String.format("Value for \"%s\"", name);

            properties.add(
                string(matchValue)
                    .label(matchValue)
                    .description(description)
                    .required(false));
        }

        return properties;
    }
}
