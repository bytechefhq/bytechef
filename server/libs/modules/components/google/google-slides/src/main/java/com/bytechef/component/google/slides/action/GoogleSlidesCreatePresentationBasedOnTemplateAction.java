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

package com.bytechef.component.google.slides.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.NAME;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.TEMPLATE_PRESENTATION_ID;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.VALUES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.slides.util.GoogleSlidesUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSlidesCreatePresentationBasedOnTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPresentationBasedOnTemplate")
        .title("Create Presentation Based on Template")
        .description(
            "Creates a new presentation based on an existing one and can replace any placeholder variables found in " +
                "your template presentation, like [[name]], [[email]], etc.")
        .properties(
            string(TEMPLATE_PRESENTATION_ID)
                .label("Template Presentation ID")
                .description("The ID of the template presentation from which the new presentation will be created.")
                .options((ActionOptionsFunction<String>) GoogleSlidesUtils::getPresentationIdOptions)
                .required(true),
            string(NAME)
                .label("New Presentation Name")
                .description("Name of the new presentation.")
                .required(true),
            object(VALUES)
                .label("Values")
                .description("Don't include the \"[[]]\", only the key name and its value.")
                .additionalProperties(string())
                .required(true))
        .output()
        .perform(GoogleSlidesCreatePresentationBasedOnTemplateAction::perform);

    private GoogleSlidesCreatePresentationBasedOnTemplateAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        File copiedPresentation = copyPresentation(
            connectionParameters, inputParameters.getRequiredString(TEMPLATE_PRESENTATION_ID),
            inputParameters.getRequiredString(NAME));

        List<Map<String, Map<String, Object>>> requests = createReplaceTextRequests(
            inputParameters.getMap(VALUES, String.class, Map.of()));

        return executeBatchUpdate(actionContext, copiedPresentation.getId(), requests);
    }

    private static File copyPresentation(Parameters connectionParameters, String templateId, String newName)
        throws Exception {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        File templatePresentation = drive.files()
            .get(templateId)
            .execute();

        File newPresentation = new File()
            .setName(newName)
            .setMimeType(templatePresentation.getMimeType());

        return drive.files()
            .copy(templateId, newPresentation)
            .execute();
    }

    private static List<Map<String, Map<String, Object>>> createReplaceTextRequests(Map<String, String> values) {
        List<Map<String, Map<String, Object>>> requests = new ArrayList<>();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            requests.add(Map.of(
                "replaceAllText", Map.of(
                    "replaceText", entry.getValue(),
                    "containsText", Map.of("text", "[[" + entry.getKey() + "]]", "matchCase", true))));
        }

        return requests;
    }

    private static Object executeBatchUpdate(
        ActionContext actionContext, String presentationId, List<Map<String, Map<String, Object>>> requests) {

        return actionContext
            .http(http -> http
                .post("https://slides.googleapis.com/v1/presentations/%s:batchUpdate".formatted(presentationId)))
            .body(Http.Body.of("requests", requests))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
