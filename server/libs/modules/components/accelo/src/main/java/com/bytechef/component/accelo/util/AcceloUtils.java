/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.accelo.util;

import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_TYPE;
import static com.bytechef.component.accelo.constant.AcceloConstants.DEPLOYMENT;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Domiter
 */
public class AcceloUtils {

    private AcceloUtils() {
    }

    public static String createUrl(Parameters connectionParameters, String resource) {
        return "https://" + connectionParameters.getRequiredString(DEPLOYMENT) + ".api.accelo.com/api/v0/" + resource;
    }

    public static List<Option<String>> getAgainstIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        String againstType = inputParameters.getRequiredString(AGAINST_TYPE);

        if (Objects.equals("company", againstType)) {
            return getCompanyIdOptions(inputParameters, connectionParameters, searchText, context);
        } else {
            Map<String, Object> body =
                context
                    .http(http -> http.get(createUrl(connectionParameters, againstType + "s")))
                    .configuration(Http.responseType(Http.ResponseType.JSON))
                    .execute()
                    .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            List<Map<String, Object>> response = (ArrayList<Map<String, Object>>) body.get("response");

            for (Map<String, Object> map : response) {
                options.add(option(String.valueOf(map.get("title")), String.valueOf(map.get("id"))));
            }

            return options;
        }
    }

    public static List<Option<String>> getCompanyIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, Object> body =
            context
                .http(http -> http.get(createUrl(connectionParameters, "companies")))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        List<Map<String, Object>> response = (ArrayList<Map<String, Object>>) body.get("response");

        for (Map<String, Object> map : response) {
            options.add(option(String.valueOf(map.get("name")), String.valueOf(map.get("id"))));
        }

        return options;
    }
}
