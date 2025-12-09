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

package com.bytechef.component.google.photos.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.PAGE_SIZE;
import static com.bytechef.component.google.photos.constant.GooglePhotosConstants.PAGE_TOKEN;

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
public class GooglePhotosUtils {

    public static List<Option<String>> getAlbumIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, ?> body = context
                .http(http -> http.get("/albums"))
                .queryParameters(PAGE_SIZE, 50, PAGE_TOKEN, nextPageToken)
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("albums") instanceof List<?> list) {
                for (Object object : list) {
                    if (object instanceof Map<?, ?> map) {
                        options.add(option((String) map.get("title"), (String) map.get("id")));
                    }
                }
            }

            nextPageToken = (String) body.get(NEXT_PAGE_TOKEN);
        } while (nextPageToken != null);

        return options;
    }
}
