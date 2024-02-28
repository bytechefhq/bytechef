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

package com.bytechef.component.keap.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class KeapCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create a Contact")
        .description("Creates a new contact")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/v1/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(array("addresses").items(object().properties(
            string("country_code").label("Country Code")
                .required(false),
            string("field").label("Field")
                .options(option("BILLING", "BILLING"), option("SHIPPING", "SHIPPING"), option("OTHER", "OTHER"))
                .required(true),
            string("line1").label("Line 1")
                .required(false),
            string("line2").label("Line 2")
                .required(false),
            string("locality").label("Locality")
                .required(false),
            string("postal_code").label("Postal Code")
                .description(
                    "Field used to store postal codes containing a combination of letters and numbers ex. 'EC1A', 'S1 2HE', '75000'")
                .required(false),
            string("region").label("Region")
                .required(false),
            string("zip_code").label("Zip Code")
                .description(
                    "Mainly used in the United States, this is typically numeric. ex. '85001', '90002' Note: this is to be used instead of 'postal_code', not in addition to.")
                .required(false),
            string("zip_four").label("Zip Four")
                .description(
                    "Last four of a full zip code ex. '8244', '4320'. This field is supplemental to the zip_code field, otherwise will be ignored.")
                .required(false)))
            .placeholder("Add to Addresses")
            .label("Addresses")
            .required(false),
            dateTime("anniversary").label("Anniversary")
                .required(false),
            dateTime("birthday").label("Birthday")
                .required(false),
            object("company").properties(integer("id").label("Id")
                .required(false))
                .label("Company")
                .required(false),
            string("contact_type").label("Contact Type")
                .required(false),
            array("custom_fields").items(object().properties(object("content").label("Content")
                .required(false),
                integer("id").label("Id")
                    .required(false)))
                .placeholder("Add to Custom Fields")
                .label("Custom Fields")
                .required(false),
            array("email_addresses").items(object().properties(string("email").label("Email")
                .required(false),
                string("field").label("Field")
                    .options(option("EMAIL1", "EMAIL1"), option("EMAIL2", "EMAIL2"), option("EMAIL3", "EMAIL3"))
                    .required(true)))
                .placeholder("Add to Email Addresses")
                .label("Email Addresses")
                .required(false),
            string("family_name").label("Family Name")
                .required(false),
            array("fax_numbers").items(object().properties(string("field").label("Field")
                .options(option("FAX1", "FAX1"), option("FAX2", "FAX2"))
                .required(true),
                string("number").label("Number")
                    .required(false),
                string("type").label("Type")
                    .required(false)))
                .placeholder("Add to Fax Numbers")
                .label("Fax Numbers")
                .required(false),
            string("given_name").label("Given Name")
                .required(false),
            string("job_title").label("Job Title")
                .required(false),
            integer("lead_source_id").label("Lead Source Id")
                .required(false),
            string("middle_name").label("Middle Name")
                .required(false),
            string("opt_in_reason").label("Opt In Reason")
                .required(false),
            object("origin").properties(string("ip_address").label("Ip Address")
                .required(true))
                .label("Origin")
                .required(false),
            integer("owner_id").label("Owner Id")
                .required(false),
            array("phone_numbers").items(object().properties(string("extension").label("Extension")
                .required(false),
                string("field").label("Field")
                    .options(option("PHONE1", "PHONE1"), option("PHONE2", "PHONE2"), option("PHONE3", "PHONE3"),
                        option("PHONE4", "PHONE4"), option("PHONE5", "PHONE5"))
                    .required(true),
                string("number").label("Number")
                    .required(false),
                string("type").label("Type")
                    .required(false)))
                .placeholder("Add to Phone Numbers")
                .label("Phone Numbers")
                .required(false),
            string("preferred_locale").label("Preferred Locale")
                .required(false)
                .exampleValue("en_US"),
            string("preferred_name").label("Preferred Name")
                .required(false),
            string("prefix").label("Prefix")
                .required(false),
            array("social_accounts").items(object().properties(string("name").label("Name")
                .required(false),
                string("type").label("Type")
                    .options(option("Facebook", "Facebook"), option("Twitter", "Twitter"),
                        option("LinkedIn", "LinkedIn"), option("Instagram", "Instagram"),
                        option("Snapchat", "Snapchat"), option("YouTube", "YouTube"), option("Pinterest", "Pinterest"))
                    .required(false)))
                .placeholder("Add to Social Accounts")
                .label("Social Accounts")
                .required(false),
            string("source_type").label("Source Type")
                .options(option("APPOINTMENT", "APPOINTMENT"), option("FORMAPIHOSTED", "FORMAPIHOSTED"),
                    option("FORMAPIINTERNAL", "FORMAPIINTERNAL"), option("WEBFORM", "WEBFORM"),
                    option("INTERNALFORM", "INTERNALFORM"), option("LANDINGPAGE", "LANDINGPAGE"),
                    option("IMPORT", "IMPORT"), option("MANUAL", "MANUAL"), option("API", "API"),
                    option("OTHER", "OTHER"), option("UNKNOWN", "UNKNOWN"))
                .required(false),
            string("spouse_name").label("Spouse Name")
                .required(false),
            string("suffix").label("Suffix")
                .required(false),
            string("time_zone").label("Time Zone")
                .required(false),
            string("website").label("Website")
                .required(false))
            .label("Item")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(string("ScoreValue").required(false), array("addresses")
                .items(object().properties(string("country_code").required(false),
                    string("field")
                        .options(option("BILLING", "BILLING"), option("SHIPPING", "SHIPPING"), option("OTHER", "OTHER"))
                        .required(true),
                    string("line1").required(false), string("line2").required(false),
                    string("locality").required(false),
                    string("postal_code").description(
                        "Field used to store postal codes containing a combination of letters and numbers ex. 'EC1A', 'S1 2HE', '75000'")
                        .required(false),
                    string("region").required(false),
                    string("zip_code").description(
                        "Mainly used in the United States, this is typically numeric. ex. '85001', '90002' Note: this is to be used instead of 'postal_code', not in addition to.")
                        .required(false),
                    string("zip_four").description(
                        "Last four of a full zip code ex. '8244', '4320'. This field is supplemental to the zip_code field, otherwise will be ignored.")
                        .required(false)))
                .required(false), dateTime("anniversary").required(false), dateTime("birthday").required(false),
                object("company").properties(string("company_name").required(false), integer("id").required(false))
                    .required(false),
                string("company_name").required(false), string("contact_type").required(false),
                array("custom_fields")
                    .items(object().properties(object("content").required(false), integer("id").required(false)))
                    .required(false),
                dateTime("date_created").required(false),
                array("email_addresses")
                    .items(object().properties(string("email").required(false),
                        string("field")
                            .options(option("EMAIL1", "EMAIL1"), option("EMAIL2", "EMAIL2"), option("EMAIL3", "EMAIL3"))
                            .required(true)))
                    .required(false),
                bool("email_opted_in").required(false),
                string("email_status")
                    .options(option("UnengagedMarketable", "UnengagedMarketable"), option("SingleOptIn", "SingleOptIn"),
                        option("DoubleOptin", "DoubleOptin"), option("Confirmed", "Confirmed"),
                        option("UnengagedNonMarketable", "UnengagedNonMarketable"),
                        option("NonMarketable", "NonMarketable"), option("Lockdown", "Lockdown"),
                        option("Bounce", "Bounce"), option("HardBounce", "HardBounce"), option("Manual", "Manual"),
                        option("Admin", "Admin"), option("System", "System"),
                        option("ListUnsubscribe", "ListUnsubscribe"), option("Feedback", "Feedback"),
                        option("Spam", "Spam"), option("Invalid", "Invalid"), option("Deactivated", "Deactivated"))
                    .required(false),
                string("family_name").required(false),
                array("fax_numbers")
                    .items(object().properties(string("field").options(option("FAX1", "FAX1"), option("FAX2", "FAX2"))
                        .required(true), string("number").required(false), string("type").required(false)))
                    .required(false),
                string("given_name").required(false), integer("id").required(false),
                string("job_title").required(false), dateTime("last_updated").required(false),
                integer("lead_source_id").required(false), string("middle_name").required(false),
                string("opt_in_reason").required(false),
                object("origin").properties(dateTime("date").required(true), string("ip_address").required(true))
                    .required(false),
                integer("owner_id").required(false),
                array("phone_numbers")
                    .items(object().properties(string("extension").required(false),
                        string("field")
                            .options(option("PHONE1", "PHONE1"), option("PHONE2", "PHONE2"), option("PHONE3", "PHONE3"),
                                option("PHONE4", "PHONE4"), option("PHONE5", "PHONE5"))
                            .required(true),
                        string("number").required(false), string("type").required(false)))
                    .required(false),
                string("preferred_locale").required(false)
                    .exampleValue("en_US"),
                string("preferred_name").required(false), string("prefix").required(false),
                array("relationships")
                    .items(object().properties(
                        integer("id").required(false), integer("linked_contact_id").required(false),
                        integer("relationship_type_id").required(false)))
                    .required(false),
                array("social_accounts").items(object().properties(string("name").required(false),
                    string("type").options(option("Facebook", "Facebook"), option("Twitter", "Twitter"),
                        option("LinkedIn", "LinkedIn"), option("Instagram", "Instagram"),
                        option("Snapchat", "Snapchat"), option("YouTube", "YouTube"), option("Pinterest", "Pinterest"))
                        .required(false)))
                    .required(false),
                string("source_type")
                    .options(option("APPOINTMENT", "APPOINTMENT"), option("FORMAPIHOSTED", "FORMAPIHOSTED"),
                        option("FORMAPIINTERNAL", "FORMAPIINTERNAL"), option("WEBFORM", "WEBFORM"),
                        option("INTERNALFORM", "INTERNALFORM"), option("LANDINGPAGE", "LANDINGPAGE"),
                        option("IMPORT", "IMPORT"), option("MANUAL", "MANUAL"), option("API", "API"),
                        option("OTHER", "OTHER"), option("UNKNOWN", "UNKNOWN"))
                    .required(false),
                string("spouse_name").required(false), string("suffix").required(false),
                array("tag_ids").items(integer(null))
                    .required(false),
                string("time_zone").required(false), string("website").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
