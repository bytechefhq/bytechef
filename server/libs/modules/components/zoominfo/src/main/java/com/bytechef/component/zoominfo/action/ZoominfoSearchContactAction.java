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

package com.bytechef.component.zoominfo.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.DEPARTMENT;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EMAIL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FIRST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FULL_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.JOB_TITLE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.LAST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_NUMBER;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_SIZE;
import static com.bytechef.component.zoominfo.util.ZoominfoUtils.checkIfNull;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZoominfoSearchContactAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchContact")
        .title("Search Contact")
        .description("Search contact by specific criteria.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Work email address for the contact in example@example.com format.")
                .required(false),
            string(FULL_NAME)
                .label("Full Name")
                .description("Contact full name.")
                .required(false),
            string(FIRST_NAME)
                .label("First Name")
                .description("Contact first name.")
                .required(false),
            string(LAST_NAME)
                .label("Last Name")
                .description("Contact last name.")
                .required(false),
            string(JOB_TITLE)
                .label("Job Title")
                .description("Contact title at current place of employment.")
                .required(false),
            string(DEPARTMENT)
                .label("Department")
                .description("Contact department at current place of employment.")
                .required(false),
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Company name.")
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(CONTACT_OUTPUT_PROPERTY)))
        .perform(ZoominfoSearchContactAction::perform);

    private ZoominfoSearchContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Object> contacts = new ArrayList<>();
        int nextPageNumber = 1;
        int pageSize = 25;
        int totalResults;

        do {
            Map<String, Object> attributes = new LinkedHashMap<>();

            checkIfNull(attributes, EMAIL, inputParameters.getString(EMAIL));
            checkIfNull(attributes, FULL_NAME, inputParameters.getString(FULL_NAME));
            checkIfNull(attributes, FIRST_NAME, inputParameters.getString(FIRST_NAME));
            checkIfNull(attributes, LAST_NAME, inputParameters.getString(LAST_NAME));
            checkIfNull(attributes, JOB_TITLE, inputParameters.getString(JOB_TITLE));
            checkIfNull(attributes, DEPARTMENT, inputParameters.getString(DEPARTMENT));
            checkIfNull(attributes, COMPANY_NAME, inputParameters.getString(COMPANY_NAME));

            Map<String, Object> data = Map.of(
                "type", "ContactSearch",
                "attributes", attributes);

            Map<String, Object> requestBody = Map.of("data", data);

            Map<String, Object> body = context.http(http -> http.post("/contacts/search"))
                .queryParameters(PAGE_SIZE, pageSize, PAGE_NUMBER, nextPageNumber)
                .body(
                    Body.of(requestBody))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("data") instanceof List<?> list) {
                contacts.addAll(list);
            }

            if (body.get("meta") instanceof Map<?, ?> meta) {
                totalResults = (Integer) meta.get("totalResults");
            } else {
                return contacts;
            }

            nextPageNumber++;

        } while ((nextPageNumber - 1) * pageSize < totalResults);

        return contacts;
    }
}
