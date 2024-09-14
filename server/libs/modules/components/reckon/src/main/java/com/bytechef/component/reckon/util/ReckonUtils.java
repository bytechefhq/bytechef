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

package com.bytechef.component.reckon.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.reckon.constant.ReckonConstants.BOOK_ID;
import static com.bytechef.component.reckon.constant.ReckonConstants.ID;
import static com.bytechef.component.reckon.constant.ReckonConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.reckon.constant.ReckonConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class ReckonUtils {

    private ReckonUtils() {
    }

    public static List<Option<String>> getBookIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/books"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, ID);

    }

    public static List<Option<String>> getCustomerOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/" + inputParameters.getRequiredString(BOOK_ID) + "/customers"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME);

    }

    public static List<Option<String>> getSupplierOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> body = context
            .http(http -> http.get("/" + inputParameters.getRequiredString(BOOK_ID) + "/suppliers"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME);
    }

    private static List<Option<String>> getOptions(Map<String, List<Map<String, Object>>> body, String value) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> map : body.get("list")) {
            options.add(option((String) map.get(NAME), (String) map.get(value)));

        }

        return options;
    }

    public static PollOutput getPollOutput(
        Parameters inputParameters, Parameters closureParameters, TriggerContext context, String path) {

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now());
        LocalDateTime endDate = LocalDateTime.now();

        // TODO check filter queryParameter and add orderBy if needed

        Http.Response response = context
            .http(http -> http.get("/" + inputParameters.getRequiredString(BOOK_ID) + "/" + path))
            .queryParameter("filter",
                URLEncoder.encode(
                    "createdDateTime ge " + startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    StandardCharsets.UTF_8))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        Map<String, List<?>> body = response.getBody(new TypeReference<>() {});

        return new PollOutput(body.get("list"), Map.of(LAST_TIME_CHECKED, endDate), false);
    }
}
