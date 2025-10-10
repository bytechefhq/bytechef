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

package com.bytechef.component.mautic.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mautic.constant.MauticConstants.ID;
import static com.bytechef.component.mautic.constant.MauticConstants.IS_PUBLISHED;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mautic.util.MauticUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MauticGetCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getCompany")
        .title("Get Company")
        .description("Get individual company.")
        .properties(
            string(ID)
                .label("Company ID")
                .description("ID of the company you want to retrieve.")
                .options((OptionsFunction<String>) MauticUtils::getCompanyOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the company."),
                        bool(IS_PUBLISHED)
                            .description("Whether the company is published."),
                        date("dateAdded")
                            .description("Date/time company was created."),
                        integer("createdBy")
                            .description("ID of the user that created the company."),
                        string("createdByUser")
                            .description("Name of the user that created the company."),
                        date("dateModified")
                            .description("Date/time company was last modified."),
                        integer("modifiedBy")
                            .description("ID of the user that last modified the company."),
                        string("modifiedByUser")
                            .description("Name of the user that last modified the company."),
                        array("fields")
                            .description("Custom fields for the company."))))
        .perform(MauticGetCompanyAction::perform);

    private MauticGetCompanyAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(
            http -> http.get("/companies/" + inputParameters.getRequiredString(ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
