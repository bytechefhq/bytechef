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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivona Pavela
 */
public class GithubGetRepositoryContentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRepositoryContent")
        .title("Get Repository Content")
        .description("Gets the contents of a file or directory in a repository." +
            " If the content is a directory, the response will be each item in the directory" +
            " and if the content is a file, the response will be file as a string.")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where the content is located.")
                .required(true),
            string(PATH)
                .label("Path")
                .description("Path to the file or the directory.")
                .required(true))
        .output(
            outputSchema(
                string()))
        .perform(GithubGetRepositoryContentAction::perform);

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object response = context
            .http(http -> http.get("/repos/" +
                inputParameters.getRequiredString(OWNER) + "/" +
                inputParameters.getRequiredString(REPOSITORY) + "/contents/" +
                inputParameters.getString(PATH)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response instanceof Map<?, ?> file) {
            String content = (String) file.get("content");

            return content == null ? "" : new String(
                context.encoder(encoder -> encoder.base64Decode(content.replaceAll("\\s", ""))),
                StandardCharsets.UTF_8);
        }

        if (response instanceof List<?> directory) {
            return directory.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(m -> m.get(NAME))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        }

        return "";
    }
}
