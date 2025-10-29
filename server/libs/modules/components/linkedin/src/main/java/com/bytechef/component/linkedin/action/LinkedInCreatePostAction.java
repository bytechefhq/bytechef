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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.ARTICLE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.AUTHOR;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COMMENTARY;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.CONTENT_TYPE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.DESCRIPTION;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.DOCUMENT;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.ID;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.IMAGES;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.SOURCE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.THUMBNAIL;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.TITLE;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.VISIBILITY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInCreatePostAction {

    protected enum Author {
        PERSON, ORGANIZATION
    }

    protected enum ContentType {
        ARTICLE, DOCUMENT, IMAGES
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPost")
        .title("Create Post")
        .description("Create a post on LinkedIn.")
        .properties(
            string(AUTHOR)
                .label("Post As")
                .description("Choose whether to post as a Person or Organization.")
                .options(
                    option("Person", Author.PERSON.name()),
                    option("Organization", Author.ORGANIZATION.name()))
                .defaultValue(Author.PERSON.name())
                .required(true),
            string(URN)
                .description("Organization URN")
                .displayCondition("%s == '%s'".formatted(AUTHOR, Author.ORGANIZATION.name()))
                .required(true),
            string(COMMENTARY)
                .label("Text")
                .description("The user generated commentary for the post.")
                .required(true),
            string(CONTENT_TYPE)
                .label("Media Category")
                .description("Type of media to be posted.")
                .options(
                    option("Article", ContentType.ARTICLE.name()),
                    option("Document", ContentType.DOCUMENT.name()),
                    option("Images", ContentType.IMAGES.name()))
                .required(false),
            string(VISIBILITY)
                .label("Visibility")
                .description("Visibility restrictions on content.")
                .options(
                    option("Public", "PUBLIC", "Anyone can view this."),
                    option("Connections only", "CONNECTIONS", "Represents 1st degree network of owner."))
                .displayCondition("%s == '%s'".formatted(AUTHOR, Author.PERSON.name()))
                .required(true),
            array(IMAGES)
                .label("Images")
                .description("Images to be posted.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.IMAGES.name()))
                .items(fileEntry(IMAGE))
                .required(true),
            string(SOURCE)
                .label("Article URL")
                .description("A URL of the article. Typically the URL that was ingested to maintain URL parameters.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.ARTICLE.name()))
                .required(true),
            string(TITLE)
                .label("Article Title")
                .description("Custom or saved title of the article.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.ARTICLE.name()))
                .maxLength(400)
                .required(false),
            string(DESCRIPTION)
                .label("Article Description")
                .description("Custom or saved description of the article.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.ARTICLE.name()))
                .maxLength(4086)
                .required(false),
            fileEntry(THUMBNAIL)
                .label("Article Thumbnail")
                .description("The thumbnail image to be associated with the article.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.ARTICLE.name()))
                .required(false),
            fileEntry(DOCUMENT)
                .label("Document")
                .description("The document to be posted.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.DOCUMENT.name()))
                .required(true),
            string(TITLE)
                .label("Document Title")
                .description("The title of the document.")
                .displayCondition("%s == '%s'".formatted(CONTENT_TYPE, ContentType.DOCUMENT.name()))
                .required(true))
        .output(outputSchema(string().description("Post ID such as urn:li:share:{id} or urn:li:ugcPost:{id})")))
        .perform(LinkedInCreatePostAction::perform);

    private LinkedInCreatePostAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String urn;
        String author = inputParameters.getRequiredString(AUTHOR);

        if (author.equals(Author.ORGANIZATION.name())) {
            urn = "urn:li:organization:" + inputParameters.getRequiredString(URN);
        } else {
            urn = getPersonUrn(connectionParameters, context);
        }

        Map<String, Object> content = buildContent(inputParameters, context, urn);

        Http.Response response = context.http(http -> http.post("/rest/posts"))
            .body(
                Http.Body.of(
                    AUTHOR, urn,
                    COMMENTARY, inputParameters.getRequiredString(COMMENTARY),
                    "distribution", Map.of("feedDistribution", "MAIN_FEED"),
                    "lifecycleState", "PUBLISHED",
                    VISIBILITY, inputParameters.getString(VISIBILITY, "PUBLIC"),
                    "content", content))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return response.getFirstHeader("x-restli-id");
    }

    private static String getPersonUrn(Parameters connectionParameters, Context context) {
        String idToken = connectionParameters.getString("id_token");

        String[] chunks = idToken.split("\\.");

        byte[] decoded = context.encoder(encoder -> encoder.urlDecode(chunks[1]));

        String payload = new String(decoded, StandardCharsets.UTF_8);

        Map<String, ?> json1 = context.json(json -> json.readMap(payload));

        return "urn:li:person:" + json1.get("sub");
    }

    private static Map<String, Object> buildContent(Parameters inputParameters, Context context, String personUrn) {
        Map<String, Object> content = null;

        String contentType = inputParameters.getString(CONTENT_TYPE);
        if (contentType != null && !contentType.isEmpty()) {
            content = new HashMap<>();

            switch (contentType) {
                case IMAGES -> {
                    List<FileEntry> images = inputParameters.getRequiredList(IMAGES, FileEntry.class);
                    if (!images.isEmpty())
                        if (images.size() == 1) {
                            FileEntry image = images.getFirst();

                            String id = LinkedInUtils.uploadContent(context, image, personUrn, contentType);

                            content.put("media", Map.of(ID, id));
                        } else {
                            List<Map<String, String>> imagesList = new ArrayList<>();

                            for (FileEntry image : images) {
                                String id = LinkedInUtils.uploadContent(context, image, personUrn, IMAGES);

                                imagesList.add(Map.of(ID, id));
                            }

                            content.put("multiImage", Map.of(IMAGES, imagesList));
                        }
                }
                case ARTICLE -> {
                    String source = inputParameters.getRequiredString(SOURCE);

                    Map<String, Object> articleMap = new HashMap<>();

                    articleMap.put(SOURCE, source);
                    articleMap.put(TITLE, inputParameters.getString(TITLE));
                    articleMap.put(DESCRIPTION, inputParameters.getString(DESCRIPTION));

                    FileEntry thumbnail = inputParameters.getFileEntry(THUMBNAIL);

                    if (thumbnail != null) {
                        String id = LinkedInUtils.uploadContent(context, thumbnail, personUrn, IMAGES);

                        articleMap.put(THUMBNAIL, id);
                    }

                    content.put(ARTICLE, articleMap);
                }
                case DOCUMENT -> {
                    FileEntry document = inputParameters.getRequiredFileEntry(DOCUMENT);
                    String id = LinkedInUtils.uploadContent(context, document, personUrn, DOCUMENT);

                    content.put("media", Map.of(ID, id, TITLE, inputParameters.getRequiredString(TITLE)));
                }
                default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
            }
        }

        return content;
    }
}
