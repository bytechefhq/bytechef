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

package com.bytechef.component.zendesk.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.ID;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.SUBJECT;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskUtils {
    private ZendeskUtils() {
    }

    public static String checkIfNull(String value) {
        return value == null ? "" : value;
    }

    public static List<Option<Long>> getTicketIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> response = context.http(http -> http.get("/tickets"))
            .configuration(responseType(ResponseType.JSON))
            .header("Accept", "application/json")
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<Long>> options = new ArrayList<>();

        if (response.get("tickets") instanceof List<?> tickets) {
            for (Object ticket : tickets) {
                if (ticket instanceof Map<?, ?> ticketMap)
                    options.add(option((String) ticketMap.get(SUBJECT), ((Integer) ticketMap.get(ID)).intValue()));
            }
        }

        return options;
    }
}
