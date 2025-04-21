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

package com.bytechef.component.apollo.util;

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
public class ApolloUtils extends AbstractApolloUtils {

    private ApolloUtils() {
    }

    public static List<Option<String>> getAccountIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions("/mixed_companies/search", "organizations", context);
    }

    public static List<Option<String>> getOwnerIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions("/users/search", "users", context);
    }

    public static List<Option<String>> getOpportunityIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return getOptions("/opportunities/search", "opportunities", context);
    }

    private static List<Option<String>> getOptions(String path, String resources, Context context) {
        Map<String, ?> body = context.http(http -> http.get(path))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(resources) instanceof List<?> users) {
            for (Object user : users) {
                if (user instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }
}
