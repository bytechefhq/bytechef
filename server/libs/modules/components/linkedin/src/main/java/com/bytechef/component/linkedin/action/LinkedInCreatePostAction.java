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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.AUTHOR;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COMMENTARY;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.DESCRIPTION;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.SOURCE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.TITLE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.VISIBILITY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInCreatePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("")
        .properties(
            string(COMMENTARY)
                .label("Text")
                .description("The user generated commentary for the post.")
                .required(true),
            string(VISIBILITY)
                .label("Visibility")
                .description("Visibility restrictions on content.")
                .options(
                    option("Public", "PUBLIC", "Anyone can view this."),
                    option("Connections only", "CONNECTIONS", "Represents 1st degree network of owner."),
                    option("Logged In", "LOGGED_IN", "Viewable by logged in members only."),
                    option("Container", "CONTAINER",
                        "Visibility is delegated to the owner of the container entity. For example, posts within a group are delegated to the groups authorization API for visibility authorization."))
                .required(true),
            fileEntry(IMAGE)
                .label("Image")
                .description("The image to be posted.")
                .required(false),
            string(SOURCE)
                .label("Article URL")
                .description("A URL of the article. Typically the URL that was ingested to maintain URL parameters.")
                .required(false),
            string(TITLE)
                .label("Article Title")
                .description("Custom or saved title of the article.")
                .maxLength(400)
                .required(false),
            string(DESCRIPTION)
                .label("Article Description")
                .description("Custom or saved description of the article.")
                .maxLength(4086)
                .required(false))
        .output()
        .perform(LinkedInCreatePostAction::perform);

    private LinkedInCreatePostAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String personUrn = getPersonUrn(connectionParameters, context);

        Map<String, Object> content = buildContent(inputParameters, context, personUrn);

        return context.http(http -> http.post("/rest/posts"))
            .body(
                Http.Body.of(
                    AUTHOR, personUrn,
                    COMMENTARY, inputParameters.getRequiredString(COMMENTARY),
                    "distribution", Map.of("feedDistribution", "MAIN_FEED"),
                    "lifecycleState", "PUBLISHED",
                    VISIBILITY, inputParameters.getRequiredString(VISIBILITY),
                    "content", content))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static String getPersonUrn(Parameters connectionParameters, Context context) {
        String idToken = connectionParameters.getString("id_token");

        String[] chunks = idToken.split("\\.");

        byte[] encoder1 = context.encoder(encoder -> encoder.urlDecode(chunks[1]));

        String payload = new String(encoder1);

        Map<String, ?> json1 = context.json(json -> json.readMap(payload));

        return "urn:li:person:" + json1.get("sub");
    }

    private static Map<String, Object> buildContent(Parameters inputParameters, Context context, String personUrn) {
        Map<String, Object> content = null;

        FileEntry image = inputParameters.getFileEntry(IMAGE);

        if (image != null) {
            Map<String, Object> upload = LinkedInUtils.uploadImage(context, image, personUrn);

            if (upload.get("value") instanceof Map<?, ?> map) {
                content = new HashMap<>();

                content.put("media", Map.of("id", (String) map.get(IMAGE)));
            }
        }

        String source = inputParameters.getString(SOURCE);
        if (source != null) {
            if (content == null) {
                content = new HashMap<>();
            }

            Map<String, Object> articleMap = new HashMap<>();

            articleMap.put(SOURCE, source);
            articleMap.put(TITLE, inputParameters.getString(TITLE));
            articleMap.put(DESCRIPTION, inputParameters.getString(DESCRIPTION));

            content.put("article", articleMap);
        }

        return content;
    }
}
