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

package com.bytechef.component.notion.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionGetPageOrBlockChildrenAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPageOrBlockChildren")
        .title("Get Page or Block Children")
        .description("Retrieve the actual content of a page (represented by blocks)")
        .properties(
            string(ID)
                .label("Page or Parent Block ID")
                .required(true))
        .output()
        .perform(NotionGetPageOrBlockChildrenAction::perform);

    private NotionGetPageOrBlockChildrenAction() {
    }

    public static List<Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String blockId = inputParameters.getRequiredString(ID);

        return getChildrenRecursively(blockId, context);
    }

    private static List<Object> getChildren(String blockId, Context context) {
        List<Object> items = new ArrayList<>();

        String startCursor = null;

        do {
            Map<String, ?> body = context
                .http(http -> http.get("/blocks/%s/children".formatted(blockId)))
                .queryParameters("page_size", 100, "start_cursor", startCursor)
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("results") instanceof List<?> list) {
                items.addAll(list);
            }

            startCursor = (String) body.get("next_cursor");
        } while (startCursor != null);
        return items;
    }

    private static List<Object> getChildrenRecursively(String blockId, Context context) {
        List<Object> children = getChildren(blockId, context);

        for (Object child : children) {
            if (child instanceof Map<?, ?> map) {
                Boolean hasChildrenObj = (Boolean) map.get("has_children");

                if (!hasChildrenObj) {
                    continue;
                }

                String id = (String) map.get("id");
                List<Object> childrenRecursively = getChildrenRecursively(id, context);

                @SuppressWarnings("unchecked")
                Map<String, Object> childrenMap = (Map<String, Object>) map;
                childrenMap.put("children", childrenRecursively);
            }
        }

        return children;
    }
}
