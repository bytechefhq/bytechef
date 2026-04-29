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

package com.bytechef.component.browser.use.util;

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.PAGE;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.PAGE_SIZE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrowserUseUtils {

    private BrowserUseUtils() {
    }

    public static List<Option<String>> getSessionIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        int pageNumber = 1;
        int defaultPageSize = 20;
        int total;

        List<Object> sessions = new ArrayList<>();
        Map<String, Object> body;

        do {
            body = context.http(http -> http.get("/sessions"))
                .queryParameters(PAGE, pageNumber, PAGE_SIZE, defaultPageSize)
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("sessions") instanceof List<?> session) {
                sessions.addAll(session);
            }

            total = (Integer) body.get("total");
            pageNumber++;

        } while ((pageNumber - 1) * defaultPageSize < total);

        List<Option<String>> options = new ArrayList<>();

        for (Object session : sessions) {
            if (session instanceof Map<?, ?> map) {
                String id = (String) map.get(ID);

                options.add(option(id, id));
            }
        }

        return options;
    }
}
