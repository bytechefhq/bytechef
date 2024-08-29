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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Utils {

    public static List<Map<?, ?>> getItemsFromNextPage(Context context, String link) {
        List<Map<?, ?>> otherItems = new ArrayList<>();

        while (link != null && !link.isEmpty()) {

            String finalLink = link;

            Map<String, Object> body = context.http(http -> http.get(finalLink))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        otherItems.add(map);
                    }
                }
            }
            link = (String) body.get(ODATA_NEXT_LINK);
        }

        return otherItems;
    }
}
