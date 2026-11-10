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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.CONTENT;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OUTPUT_FORMAT;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REF;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GithubGetRepositoryContentAction {

    public static final String STRUCTURED = "STRUCTURED";
    public static final String TEXT = "TEXT";

    private static final List<String> ENTRY_KEYS = List.of(
        NAME, PATH, "sha", "size", "type", "html_url", "download_url");

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRepositoryContent")
        .title("Get Repository Content")
        .description(
            "Gets the contents of a file or directory in a repository. With the Text output format a directory " +
                "returns its entry names, one per line, and a file returns its content as a string. With the " +
                "Structured output format a directory returns a list of entries and a file returns an object with " +
                "its metadata and decoded content.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#get-repository-content")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where the content is located.")
                .required(true),
            string(PATH)
                .label("Path")
                .description("Path to the file or the directory.")
                .required(true),
            string(REF)
                .label("Ref")
                .description(
                    "The branch, tag or commit SHA to read from. Defaults to the repository's default branch.")
                .required(false),
            string(OUTPUT_FORMAT)
                .label("Output Format")
                .description(
                    "Text returns a single string. Structured returns the entries of a directory, or the metadata " +
                        "and decoded content of a file.")
                .options(
                    option("Text", TEXT, "A string: entry names for a directory, the content for a file."),
                    option("Structured", STRUCTURED, "Entries with name, path, type, size and URLs."))
                .defaultValue(TEXT)
                .required(false))
        .output(GithubGetRepositoryContentAction::output)
        .perform(GithubGetRepositoryContentAction::perform);

    private GithubGetRepositoryContentAction() {
    }

    public static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        if (isStructured(inputParameters)) {
            return OutputResponse.of(perform(inputParameters, connectionParameters, context));
        }

        return OutputResponse.of(string());
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Object response = getContent(inputParameters, context);

        if (isStructured(inputParameters)) {
            return toStructured(response, context);
        }

        if (response instanceof Map<?, ?> file) {
            return decodeContent(file, context);
        }

        if (response instanceof List<?> directory) {
            return directory.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(entry -> entry.get(NAME))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        }

        return "";
    }

    private static String decodeContent(Map<?, ?> file, Context context) {
        if (!(file.get(CONTENT) instanceof String content) || content.isEmpty()) {
            return "";
        }

        return new String(
            context.encoder(encoder -> encoder.base64Decode(content.replaceAll("\\s", ""))), StandardCharsets.UTF_8);
    }

    private static Object getContent(Parameters inputParameters, Context context) {
        Executor executor = context.http(http -> http.get(
            "/repos/%s/%s/contents/%s".formatted(
                inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY),
                inputParameters.getString(PATH))));

        String ref = inputParameters.getString(REF);

        if (ref != null && !ref.isBlank()) {
            executor = executor.queryParameters(REF, ref);
        }

        return executor.configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static boolean isStructured(Parameters inputParameters) {
        return STRUCTURED.equals(inputParameters.getString(OUTPUT_FORMAT, TEXT));
    }

    private static Map<String, Object> toEntry(Map<?, ?> item) {
        Map<String, Object> entry = new HashMap<>();

        for (String key : ENTRY_KEYS) {
            entry.put(key, item.get(key));
        }

        return entry;
    }

    private static Object toStructured(Object response, Context context) {
        if (response instanceof Map<?, ?> file) {
            Map<String, Object> entry = toEntry(file);

            entry.put(CONTENT, decodeContent(file, context));

            return entry;
        }

        if (response instanceof List<?> directory) {
            return directory.stream()
                .filter(Map.class::isInstance)
                .map(item -> toEntry((Map<?, ?>) item))
                .toList();
        }

        return Map.of();
    }
}
