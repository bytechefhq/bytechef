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

package com.bytechef.component.zoominfo.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class ZoominfoConstants {

    public static final String BUSINESS_MODEL = "businessModel";
    public static final String COMPANY_CITY = "companyCity";
    public static final String COMPANY_COUNTRY = "companyCountry";
    public static final String COMPANY_DESCRIPTION = "companyDescription";
    public static final String COMPANY_ID = "companyId";
    public static final String COMPANY_NAME = "companyName";
    public static final String COMPANY_PHONE = "companyPhone";
    public static final String COMPANY_STREET = "companyStreet";
    public static final String COMPANY_STATE = "companyState";
    public static final String COMPANY_TYPE = "companyType";
    public static final String COMPANY_WEBSITE = "companyWebsite";
    public static final String COMPANY_ZIPCODE = "companyZipcode";
    public static final String COUNTRY = "country";
    public static final String DEPARTMENT = "department";
    public static final String EMAIL = "emailAddress";
    public static final String EXTERNAL_URL = "externalURL";
    public static final String FIRST_NAME = "firstName";
    public static final String FULL_NAME = "fullName";
    public static final String JOB_TITLE = "jobTitle";
    public static final String LAST_NAME = "lastName";
    public static final String OUTPUT_FIELDS = "outputFields";
    public static final String PERSON_ID = "personId";
    public static final String PHONE = "phone";

    public static final ModifiableObjectProperty CONTACT_OUTPUT_PROPERTY =
        object()
            .properties(
                integer("personId")
                    .description("Unique ZoomInfo identifier for the contact."),
                string("firstName")
                    .description("Contact first name."),
                string("lastName")
                    .description("Contact last name."),
                string("middleName")
                    .description("Contact middle name."),
                string("companyId")
                    .description("ZoomInfo unique identifier for the company."),
                string("companyName")
                    .description("Company name."),
                string("jobTitle")
                    .description("Contact job title at current place of employment."),
                integer("contactAccuracyScore")
                    .description(
                        "This score indicates the likelihood that a contact is reachable and still employed by the company listed. Minimum score is 75 and maximum is 99."),
                string("validDate")
                    .description("Date on which the contact record was last validated."),
                string("lastUpdatedDate")
                    .description("Date on which the contact record was last updated."),
                bool("hasEmail")
                    .description("Indicates whether ZoomInfo has an email address for the contact."),
                bool("hasSupplementalEmail")
                    .description("Indicates whether ZoomInfo has a supplemental email address for the contact."),
                bool("hasDirectPhone")
                    .description("Indicates whether ZoomInfo has a direct phone number for the contact."),
                bool("hasMobilePhone")
                    .description("Indicates whether ZoomInfo has a mobile phone number for the contact."),
                bool("hasCompanyIndustry")
                    .description("Indicates whether ZoomInfo has company industry for the contact."),
                bool("hasCompanyPhone")
                    .description("Indicates whether ZoomInfo has a company phone number for the contact."),
                bool("hasCompanyStreet")
                    .description("Indicates whether ZoomInfo has a street address for the contact."),
                bool("hasCompanyState")
                    .description("Indicates whether ZoomInfo has a state for the contact."),
                bool("hasCompanyZipCode")
                    .description("Indicates whether ZoomInfo has a Zip Code or Postal Code for the contact."),
                bool("hasCompanyCountry")
                    .description("Indicates whether ZoomInfo has a country for the contact."),
                bool("hasCompanyRevenue")
                    .description("Indicates whether ZoomInfo has company revenue data for the contact."),
                bool("hasCompanyEmployeeCount")
                    .description("Indicates whether ZoomInfo has company headcount data for the contact"));

    private ZoominfoConstants() {
    }
}
