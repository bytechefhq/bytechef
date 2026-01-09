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
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.BUSINESS_MODEL;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_DESCRIPTION;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_NAME;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_OUTPUT_PROPERTY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COMPANY_TYPE;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.COUNTRY;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_NUMBER;
import static com.bytechef.component.zoominfo.constant.ZoominfoConstants.PAGE_SIZE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class ZoominfoSearchCompanyAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchCompany")
        .title("Search Company")
        .description("Search company by specific criteria.")
        .properties(
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Company name.")
                .required(false),
            string(COMPANY_DESCRIPTION)
                .label("Company Description")
                .description("Text description unique to the company you want to use as search criteria.")
                .required(false),
            string(COMPANY_TYPE)
                .label("Company Type")
                .description("Company type (private, public, and so on).")
                .required(false),
            string(BUSINESS_MODEL)
                .label("Business Model")
                .description("Search using Business Model (B2C, B2B, B2G) for a company. Default is All.")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("Country for the company's primary address.")
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(COMPANY_OUTPUT_PROPERTY)))
        .perform(ZoominfoSearchCompanyAction::perform);

    private ZoominfoSearchCompanyAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Object> companies = new ArrayList<>();
        int nextPageNumber = 1;
        int pageSize = 25;
        int totalResults;

        do {
            Map<String, Object> body = context.http(http -> http.post("/companies/search"))
                .queryParameters(PAGE_SIZE, pageSize, PAGE_NUMBER, nextPageNumber)
                .body(
                    Body.of(
                        Map.of(
                            "data", Map.of(
                                "type", "CompanySearch",
                                "attributes",
                                Map.of(
                                    COMPANY_NAME, inputParameters.getString(COMPANY_NAME),
                                    COMPANY_DESCRIPTION, inputParameters.getString(COMPANY_DESCRIPTION),
                                    COMPANY_TYPE, inputParameters.getString(COMPANY_TYPE),
                                    BUSINESS_MODEL, inputParameters.getString(BUSINESS_MODEL),
                                    COUNTRY, inputParameters.getString(COUNTRY))))))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("data") instanceof List<?> list) {
                companies.addAll(list);
            }

            if (body.get("meta") instanceof Map<?, ?> meta) {
                totalResults = (Integer) meta.get("totalResults");
            } else {
                return companies;
            }

            nextPageNumber++;

        } while ((nextPageNumber - 1) * pageSize < totalResults);

        return companies;
    }
}
