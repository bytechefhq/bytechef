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

package com.bytechef.microsoft.commons;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftUtils {

    public static final String ODATA_NEXT_LINK = "@odata.nextLink";
    public static final String VALUE = "value";

    public static List<Option<String>> getOptions(
        Context context, Map<String, ?> body, String label, String value) {

        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(label), (String) map.get(value)));
                }
            }
        }

        List<Map<?, ?>> itemsFromNextPage = getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context);

        for (Map<?, ?> map : itemsFromNextPage) {
            options.add(option((String) map.get(label), (String) map.get(value)));
        }

        return options;
    }

    public static List<Map<?, ?>> getItemsFromNextPage(String link, Context context) {
        List<Map<?, ?>> otherItems = new ArrayList<>();

        while (link != null && !link.isEmpty()) {
            String finalLink = link;

            Map<String, Object> body = context.http(http -> http.get(finalLink))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        otherItems.add(map);
                    }
                }
            }

            link = (String) body.get(ODATA_NEXT_LINK);
        }

        return otherItems;
    }

    public static ProviderException processErrorResponse(int statusCode, Object body, Context context) {
        String message;

        Object json = context.json(json1 -> json1.read((String) body));

        if (json instanceof Map<?, ?> map && map.get("error") instanceof Map<?, ?> errorMap) {
            message = (String) errorMap.get("message");
        } else {
            message = body == null ? null : body.toString();
        }

        return new ProviderException(statusCode, message);
    }

    private MicrosoftUtils() {
    }
}
