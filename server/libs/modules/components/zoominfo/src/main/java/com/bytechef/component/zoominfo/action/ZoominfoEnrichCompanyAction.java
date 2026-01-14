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
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_CITY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_COUNTRY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ID;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_PHONE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_STATE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_STREET;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_WEBSITE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_ZIPCODE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.OUTPUT_FIELDS;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.zoominfo.util.ZoominfoUtils;

/**
 * @author Marija Horvat
 */
public class ZoominfoEnrichCompanyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("enrichCompany")
        .title("Enrich Company")
        .description("Enrich company details.")
        .properties(
            integer(COMPANY_ID)
                .label("Company ID")
                .description("Unique ZoomInfo identifier for a company.")
                .required(true),
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Company name.")
                .required(false),
            string(COMPANY_WEBSITE)
                .label("Company Website")
                .description("Company website URL in http://www.example.com format.")
                .required(false),
            string(COMPANY_PHONE)
                .label("Company Phone")
                .description("Phone number of the company headquarters.")
                .required(false),
            string(COMPANY_STREET)
                .label("Company Street")
                .description("Street address for the company's primary address.")
                .required(false),
            string(COMPANY_CITY)
                .label("Company City")
                .description("City for the company's primary address.")
                .required(false),
            string(COMPANY_STATE)
                .label("Company State")
                .description("State for the company's primary address.")
                .required(false),
            string(COMPANY_ZIPCODE)
                .label("Company Zip Code")
                .description("Zip code or postal code for the company's primary address.")
                .required(false),
            string(COMPANY_COUNTRY)
                .label("Company Country")
                .description("Country for the company's primary address.")
                .required(false),
            array(OUTPUT_FIELDS)
                .label("Output Fields")
                .description("Fields you want to get from employee.")
                .items(string())
                .options((ActionDefinition.OptionsFunction<String>) ZoominfoUtils::getCompanyFieldOptions)
                .required(false))
        .output()
        .perform(ZoominfoEnrichCompanyAction::perform);

    private ZoominfoEnrichCompanyAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/companies/enrich"))
            .body(
                Body.of("data", new Object[] {
                    "type", "CompanyEnrich",
                    "attributes",
                    new Object[] {
                        OUTPUT_FIELDS, inputParameters.getList(OUTPUT_FIELDS),
                        "matchCompanyInput", new Object[] {
                            COMPANY_ID, inputParameters.getRequiredInteger(COMPANY_ID),
                            COMPANY_NAME, inputParameters.getString(COMPANY_NAME),
                            COMPANY_WEBSITE, inputParameters.getString(COMPANY_WEBSITE),
                            COMPANY_PHONE, inputParameters.getString(COMPANY_PHONE),
                            COMPANY_STREET, inputParameters.getString(COMPANY_STREET),
                            COMPANY_CITY, inputParameters.getString(COMPANY_CITY),
                            COMPANY_STATE, inputParameters.getString(COMPANY_STATE),
                            COMPANY_ZIPCODE, inputParameters.getString(COMPANY_ZIPCODE),
                            COMPANY_COUNTRY, inputParameters.getString(COMPANY_COUNTRY)
                        }
                    }
                }))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
