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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EMAIL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.EXTERNAL_URL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FIRST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.FULL_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.JOB_TITLE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.LAST_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.OUTPUT_FIELDS;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PERSON_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PHONE;
import static com.bytechef.component.zoominfo.util.ZoominfoUtils.checkIfNull;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.zoominfo.util.ZoominfoUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZoominfoEnrichContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("enrichContact")
        .title("Enrich Contact")
        .description("Enrich contact details.")
        .properties(
            integer(PERSON_ID)
                .label("Person ID")
                .description("Unique ZoomInfo identifier for the contact.")
                .required(true),
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
            string(EMAIL)
                .label("Email")
                .description("Business or Personal email address for the contact in example@example.com format.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("Contact direct or mobile phone number.")
                .required(false),
            string(JOB_TITLE)
                .label("Job Title")
                .description("Contact title at current place of employment.")
                .required(false),
            string(EXTERNAL_URL)
                .label("External URL")
                .description("Social media URLs for the contact (e.g., Facebook, Twitter, LinkedIn).")
                .required(false),
            integer(COMPANY_ID)
                .label("Company ID")
                .description("Unique ZoomInfo identifier for the company.")
                .required(false),
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Name of the company for for which the contact works, or has worked.")
                .required(false),
            array(OUTPUT_FIELDS)
                .label("Output Fields")
                .description("Fields you want to get from employee. See documentation for available fields.")
                .items(string())
                .options((OptionsFunction<String>) ZoominfoUtils::getContactFieldOptions)
                .required(false))
        .output()
        .perform(ZoominfoEnrichContactAction::perform);

    private ZoominfoEnrichContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> matchPersonInput = ZoominfoUtils.createPropertyMap(inputParameters, FULL_NAME,
            FIRST_NAME, LAST_NAME, EMAIL, PHONE, JOB_TITLE, EXTERNAL_URL, COMPANY_NAME);

        checkIfNull(matchPersonInput, PERSON_ID, inputParameters.getRequiredInteger(PERSON_ID));
        checkIfNull(matchPersonInput, COMPANY_ID, inputParameters.getInteger(COMPANY_ID));

        Map<String, Object> attributes = new HashMap<>();

        checkIfNull(attributes, OUTPUT_FIELDS, inputParameters.getList(OUTPUT_FIELDS));
        attributes.put("matchPersonInput", matchPersonInput);

        return context.http(http -> http.post("/contacts/enrich"))
            .body(
                Body.of(
                    "data", new Object[] {
                        "type", "ContactEnrich",
                        "attributes", attributes
                    }))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
