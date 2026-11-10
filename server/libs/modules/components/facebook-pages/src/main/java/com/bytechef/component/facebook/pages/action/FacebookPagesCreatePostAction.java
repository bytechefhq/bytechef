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

package com.bytechef.component.facebook.pages.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.LINK;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.MESSAGE;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.facebook.pages.util.FacebookPagesUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesCreatePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("Publishes a text post, optionally with a link, to a Facebook Page.")
        .help("", "https://docs.bytechef.io/reference/components/facebook-pages_v1#create-post")
        .properties(
            PAGE_ID_PROPERTY,
            string(MESSAGE)
                .label("Message")
                .description("The text of the post.")
                .required(true),
            string(LINK)
                .label("Link")
                .description("The URL to attach to the post.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the created post."))))
        .perform(FacebookPagesCreatePostAction::perform);

    private FacebookPagesCreatePostAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String pageId = inputParameters.getRequiredString(PAGE_ID);

        String pageAccessToken = FacebookPagesUtils.getPageAccessToken(context, pageId);

        Map<String, Object> body = new HashMap<>();

        body.put(MESSAGE, inputParameters.getRequiredString(MESSAGE));

        String link = inputParameters.getString(LINK);

        if (link != null && !link.isBlank()) {
            body.put(LINK, link);
        }

        return FacebookPagesUtils.executePageRequest(
            context.http(http -> http.post("/" + pageId + "/feed"))
                .body(Http.Body.of(body)),
            pageAccessToken);
    }
}
