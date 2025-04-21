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

package com.bytechef.component.todoist.util;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TodoistUtils extends AbstractTodoistUtils {

    private TodoistUtils() {
    }

    public static List<Option<String>> getTaskIdOptions(
        Parameters inputParameters,
        Parameters connectionParameters, Map<String, String> lookupDependsOnPaths, String searchText,
        Context context) {

        return getOptions(context, "/tasks", "content");
    }

    public static List<Option<String>> getProjectIdOptions(
        Parameters inputParameters,
        Parameters connectionParameters, Map<String, String> lookupDependsOnPaths, String searchText,
        Context context) {

        return getOptions(context, "/projects", "name");

    }

    private static List<Option<String>> getOptions(Context context, String path, String label) {
        List<Map<String, Object>> body = context
            .http(http -> http.get(path))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body) {
            options.add(option((String) map.get(label), (String) map.get("id")));
        }

        return options;
    }
}
