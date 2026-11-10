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
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.CAPTION;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID_PROPERTY;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.URL;

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
public class FacebookPagesCreatePhotoPostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPhotoPost")
        .title("Create Photo Post")
        .description("Publishes a photo from a public URL to a Facebook Page.")
        .help("", "https://docs.bytechef.io/reference/components/facebook-pages_v1#create-photo-post")
        .properties(
            PAGE_ID_PROPERTY,
            string(URL)
                .label("Image URL")
                .description("The public URL of the image to publish.")
                .required(true),
            string(CAPTION)
                .label("Caption")
                .description("The text that accompanies the photo.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the uploaded photo."),
                        string("post_id")
                            .description("The ID of the Page post that contains the photo."))))
        .perform(FacebookPagesCreatePhotoPostAction::perform);

    private FacebookPagesCreatePhotoPostAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String pageId = inputParameters.getRequiredString(PAGE_ID);

        String pageAccessToken = FacebookPagesUtils.getPageAccessToken(context, pageId);

        Map<String, Object> body = new HashMap<>();

        body.put(URL, inputParameters.getRequiredString(URL));

        String caption = inputParameters.getString(CAPTION);

        if (caption != null && !caption.isBlank()) {
            body.put(CAPTION, caption);
        }

        return FacebookPagesUtils.executePageRequest(
            context.http(http -> http.post("/" + pageId + "/photos"))
                .body(Http.Body.of(body)),
            pageAccessToken);
    }
}
