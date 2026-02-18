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

package com.bytechef.component.linkedin.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class LinkedInDeletePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deletePost")
        .title("Delete Post")
        .description("Delete a post from LinkedIn.")
        .properties(
            string(URN)
                .label("URN")
                .description("ugcPostUrn|shareUrn")
                .required(true))
        .perform(LinkedInDeletePostAction::perform);

    private LinkedInDeletePostAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String urn = context.encoder(encoder -> encoder.base64UrlEncode(inputParameters.getRequiredString(URN)));

        context.http(http -> http.delete("/rest/posts/" + urn))
            .execute();

        return null;
    }

}
