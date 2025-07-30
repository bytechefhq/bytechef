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

package com.bytechef.component.urlscan.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.urlscan.constant.UrlscanConstants.SCAN_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class UrlscanScreenshotAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("screenshot")
        .title("Screenshot")
        .description("Use the scan ID to retrieve the screenshot for a scan once the scan has finished.")
        .properties(
            string(SCAN_ID)
                .label("Scan ID")
                .description("UUID of scan result.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("Screenshot that was created.")))
        .perform(UrlscanScreenshotAction::perform);

    private UrlscanScreenshotAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(
            http -> http.get("/screenshots/%s.png".formatted(inputParameters.getRequiredString(SCAN_ID))))
            .configuration(responseType(ResponseType.binary("image/png")))
            .execute()
            .getBody();
    }
}
