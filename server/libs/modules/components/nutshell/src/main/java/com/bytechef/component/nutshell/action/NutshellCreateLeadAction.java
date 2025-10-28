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

package com.bytechef.component.nutshell.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.nutshell.constant.NutshellConstants.DESCRIPTION;
import static com.bytechef.component.nutshell.constant.NutshellConstants.ID;
import static com.bytechef.component.nutshell.constant.NutshellConstants.LINKS;
import static com.bytechef.component.nutshell.constant.NutshellConstants.NAME;
import static com.bytechef.component.nutshell.constant.NutshellConstants.OWNER;
import static com.bytechef.component.nutshell.util.NutshellUtils.addIfPresent;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.nutshell.util.NutshellUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kalaiyarasan Raja
 */
public class NutshellCreateLeadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createLead")
        .title("Create Lead")
        .description("Creates new Lead")
        .properties(
            string(DESCRIPTION)
                .label("Name|Description")
                .description("Description of the lead, which is also set as the name of the lead")
                .required(true),
            string(OWNER)
                .options((OptionsFunction<String>) NutshellUtils::getUserOptions)
                .label("Assignee")
                .description("The user to whom the lead is assigned")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("leads")
                            .items(
                                object()
                                    .properties(
                                        string(ID)
                                            .description("ID of the lead"),
                                        string("type")
                                            .description("The type of this entity, e.g. 'contacts', 'leads'."),
                                        string(NAME)
                                            .description("The full name of the lead."),
                                        string(DESCRIPTION)
                                            .description(
                                                "Description of the lead, which appears under their name."))))))
        .perform(NutshellCreateCompanyAction::perform);

    private NutshellCreateLeadAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> leadMap = createLeadMap(inputParameters);

        return actionContext
            .http(http -> http.post("/leads"))
            .body(Http.Body.of("leads", List.of(leadMap)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> createLeadMap(Parameters inputParameters) {
        Map<String, Object> leadMap = new HashMap<>();

        leadMap.put(DESCRIPTION, inputParameters.getRequiredString(DESCRIPTION));

        addIfPresent(inputParameters, OWNER, LINKS, leadMap);

        return leadMap;
    }
}
