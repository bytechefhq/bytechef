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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.COMMENT_FIELDS;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.COMMENT_OUTPUT_PROPERTY;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID_PROPERTY;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.POST_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.facebook.pages.util.FacebookPagesUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesGetCommentsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getComments")
        .title("Get Comments")
        .description("Gets the comments on a post of a Facebook Page.")
        .help("", "https://docs.bytechef.io/reference/components/facebook-pages_v1#get-comments")
        .properties(
            PAGE_ID_PROPERTY,
            string(POST_ID)
                .label("Post ID")
                .description("The ID of the post, in the {page-id}_{post-id} format returned by Create Post.")
                .required(true))
        .output(outputSchema(array().items(COMMENT_OUTPUT_PROPERTY)))
        .perform(FacebookPagesGetCommentsAction::perform);

    private FacebookPagesGetCommentsAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String pageAccessToken = FacebookPagesUtils.getPageAccessToken(
            context, inputParameters.getRequiredString(PAGE_ID));

        String postId = inputParameters.getRequiredString(POST_ID);

        Map<String, Object> body = FacebookPagesUtils.executePageRequest(
            context.http(http -> http.get("/" + postId + "/comments"))
                .queryParameters("fields", COMMENT_FIELDS, "limit", 100),
            pageAccessToken);

        return FacebookPagesUtils.getDataList(body);
    }
}
