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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Monika Kušter
 */
public class GoogleSlidesGetPresentationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPresentation")
        .title("Get Presentation")
        .description("Gets the latest version of the specified presentation.")
        .help("", "https://docs.bytechef.io/reference/components/google-slides_v1#get-presentation")
        .properties(
            string(FILE_ID)
                .label("Presentation ID")
                .description("The ID of the presentation.")
                .options(GoogleUtils.getFileOptionsByMimeType("application/vnd.google-apps.presentation", true))
                .required(true))
        .output()
        .perform(GoogleSlidesGetPresentationAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleSlidesGetPresentationAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.get("/presentations/%s".formatted(inputParameters.getRequiredString(FILE_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
