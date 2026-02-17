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

package com.bytechef.component.google.slides.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.PLACEHOLDER_FORMAT;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.VALUES;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_NAME;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FOLDER_ID;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.slides.util.GoogleSlidesUtils;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class GoogleSlidesCreatePresentationFromTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPresentationFromTemplate")
        .title("Create Presentation From Template")
        .description(
            "Creates a new presentation based on an existing one and can replace any placeholder variables found in " +
                "your template presentation, like [[name]], [[email]], etc.")
        .help("", "https://docs.bytechef.io/reference/components/google-slides_v1#create-presentation-from-template")
        .properties(
            string(FILE_ID)
                .label("Template Presentation ID")
                .description("The ID of the template presentation from which the new presentation will be created.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.presentation", true))
                .required(true),
            string(PLACEHOLDER_FORMAT)
                .label("Placeholder Format")
                .description("Choose the format of placeholders in your template.")
                .options(
                    option("Curly Braces {{}}", "{{}}"),
                    option("Square Brackets [[]]", "[[]]"))
                .defaultValue("{{}}")
                .required(true),
            string(FILE_NAME)
                .label("Title of New Presentation")
                .description("Name of the new presentation.")
                .required(true),
            string(FOLDER_ID)
                .label("Folder ID")
                .description(
                    "ID of the folder where the new presentation will be saved. If not provided, the new " +
                        "presentation will be saved in the same folder as the template presentation.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.folder", true))
                .required(false),
            dynamicProperties(VALUES)
                .properties((PropertiesFunction) GoogleSlidesUtils::createPropertiesForPlaceholderVariables)
                .propertiesLookupDependsOn(FILE_ID, PLACEHOLDER_FORMAT))
        .output()
        .perform(GoogleSlidesCreatePresentationFromTemplateAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleSlidesCreatePresentationFromTemplateAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        File copiedPresentation = GoogleUtils.copyFileOnGoogleDrive(connectionParameters, inputParameters);

        List<Map<String, Map<String, Object>>> requests = createReplaceTextRequests(
            inputParameters.getMap(VALUES, String.class, Map.of()),
            inputParameters.getRequiredString(PLACEHOLDER_FORMAT));

        return executeBatchUpdate(context, copiedPresentation.getId(), requests);
    }

    private static List<Map<String, Map<String, Object>>> createReplaceTextRequests(
        Map<String, String> values, String placeholderFormat) {

        boolean isBracketFormat = "[[]]".equals(placeholderFormat);

        return values.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> {
                String key = entry.getKey();
                String textToReplace = isBracketFormat
                    ? "[[" + key + "]]"
                    : "{{" + key + "}}";

                Map<String, Object> containsText = Map.of(
                    "text", textToReplace,
                    "matchCase", true);

                Map<String, Object> replaceAllText = Map.of(
                    "replaceText", entry.getValue(),
                    "containsText", containsText);

                return Map.of("replaceAllText", replaceAllText);
            })
            .collect(Collectors.toList());
    }

    private static Object executeBatchUpdate(
        Context context, String presentationId, List<Map<String, Map<String, Object>>> requests) {

        return context
            .http(http -> http.post("/presentations/%s:batchUpdate".formatted(presentationId)))
            .body(Http.Body.of("requests", requests))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
