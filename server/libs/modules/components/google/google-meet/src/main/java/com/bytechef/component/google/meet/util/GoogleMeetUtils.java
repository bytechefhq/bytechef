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

package com.bytechef.component.google.meet.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_SIZE;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_TOKEN;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class GoogleMeetUtils {

    public static List<Option<String>> getConferenceRecordsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, ?> body = context
                .http(http -> http.get("https://meet.googleapis.com/v2/conferenceRecords"))
                .queryParameters(PAGE_SIZE, 100, PAGE_TOKEN, nextPageToken)
                .configuration(responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("conferenceRecords") instanceof List<?> list) {
                for (Object object : list) {
                    if (object instanceof Map<?, ?> map) {
                        String name = (String) map.get("name");

                        options.add(option(name.substring("conferenceRecords/".length()), name));
                    }
                }
            }

            nextPageToken = (String) body.get(NEXT_PAGE_TOKEN);
        } while (nextPageToken != null);

        return options;
    }
}
