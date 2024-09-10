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

package com.bytechef.component.microsoft.share.point.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.COLUMNS;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.CREATE_LIST_ITEM;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.FIELDS;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.LIST_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointCreateListItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_LIST_ITEM)
        .title("Create list item")
        .description("Creates a new item in a list.")
        .properties(
            SITE_ID_PROPERTY,
            string(LIST_ID)
                .label("List")
                .optionsLookupDependsOn(SITE_ID)
                .options((ActionOptionsFunction<String>) MicrosoftSharePointUtils::getListIdOptions)
                .required(true),
            dynamicProperties(COLUMNS)
                .propertiesLookupDependsOn(SITE_ID, LIST_ID)
                .properties(MicrosoftSharePointUtils::createPropertiesForListItem))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID))))
        .perform(MicrosoftSharePointCreateListItemAction::perform);

    private MicrosoftSharePointCreateListItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, ?> map = inputParameters.getMap(COLUMNS, Map.of());

        List<Object> objects = new ArrayList<>();

        map.forEach((key, value) -> {
            objects.add(key);
            objects.add(value);
        });

        return context
            .http(http -> http.post(
                "/" + inputParameters.getRequiredString(SITE_ID) + "/lists/" +
                    inputParameters.getRequiredString(LIST_ID) + "/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(FIELDS, objects.toArray()))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
