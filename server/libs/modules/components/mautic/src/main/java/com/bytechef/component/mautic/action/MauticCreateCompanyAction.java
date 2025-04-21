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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mautic.constant.MauticConstants.COMPANY_NAME;
import static com.bytechef.component.mautic.constant.MauticConstants.IS_PUBLISHED;
import static com.bytechef.component.mautic.constant.MauticConstants.OVERWRITE_WITH_BLANK;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MauticCreateCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create Company")
        .description("Creates a new company.")
        .properties(
            string(COMPANY_NAME)
                .label("Company Name")
                .required(true),
            integer(IS_PUBLISHED)
                .label("Is Published")
                .description("Will the company be published after creation.")
                .options(
                    List.of(
                        option("true", 1),
                        option("false", 0)))
                .required(true),
            bool(OVERWRITE_WITH_BLANK)
                .label("Overwrite With Blank")
                .description(
                    "If true, then empty values are set to fields." +
                        "Otherwise empty values are skipped.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
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
        .perform(MauticCreateCompanyAction::perform);

    private MauticCreateCompanyAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/companies/new"))
            .body(
                Body.of(
                    COMPANY_NAME, inputParameters.getRequiredString(COMPANY_NAME),
                    IS_PUBLISHED, inputParameters.getRequiredInteger(IS_PUBLISHED),
                    OVERWRITE_WITH_BLANK, inputParameters.getBoolean(OVERWRITE_WITH_BLANK)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
