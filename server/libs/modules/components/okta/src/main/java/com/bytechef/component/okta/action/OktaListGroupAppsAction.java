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

package com.bytechef.component.okta.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.okta.constant.OktaConstants.APPLICATION_OUTPUT_PROPERTY;
import static com.bytechef.component.okta.constant.OktaConstants.GROUP_ID;
import static com.bytechef.component.okta.constant.OktaConstants.LIMIT;
import static com.bytechef.component.okta.constant.OktaConstants.LIMIT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.okta.util.OktaUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class OktaListGroupAppsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listGroupApps")
        .title("List Group Apps")
        .description("List the apps assigned to the group.")
        .properties(
            string(GROUP_ID)
                .label("Group ID")
                .description("The ID of the group.")
                .options((OptionsFunction<String>) OktaUtils::getGroupIdOptions)
                .required(true),
            LIMIT_PROPERTY)
        .output(outputSchema(array().items(APPLICATION_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/okta_v1#list-group-apps")
        .perform(OktaListGroupAppsAction::perform);

    private OktaListGroupAppsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String path = "/groups/" + OktaUtils.encodePathSegment(inputParameters.getRequiredString(GROUP_ID)) + "/apps";

        return OktaUtils.getItems(context, path, Map.of(), inputParameters.getInteger(LIMIT));
    }
}
