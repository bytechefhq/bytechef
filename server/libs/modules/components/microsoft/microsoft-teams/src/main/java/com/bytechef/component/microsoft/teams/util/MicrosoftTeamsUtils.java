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

package com.bytechef.component.microsoft.teams.util;

import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsUtils {

    private MicrosoftTeamsUtils() {
    }

    public static List<String> getChatMembers(ActionContext context, Map<?, ?> map) {
        List<String> members = new ArrayList<>();

        Map<String, Object> body = context.http(http -> http.get("/chats/" + map.get(ID) + "/members"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> memberMap) {
                    members.add((String) memberMap.get(DISPLAY_NAME));
                }
            }
        }

        return members;
    }
}
