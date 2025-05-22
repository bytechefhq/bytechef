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

package com.bytechef.component.agile.crm.action;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.ADDRESS;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.CITY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.COMPANY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.COUNTRY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.EMAIL;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.FIRST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.LAST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PHONE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PROPERTIES;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.STATE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.TAGS;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.WEBSITE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.ZIP_CODE;
import static com.bytechef.component.agile.crm.util.AgileCrmUtils.getPropertiesList;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .properties(
            string(FIRST_NAME)
                .label("First Name")
                .description("The first name of the contact.")
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("The last name of the contact.")
                .required(false),
            string(EMAIL)
                .label("Email")
                .description("Email of the contact.")
                .required(true),
            string(ADDRESS)
                .label("Address")
                .description("The address of the contact.")
                .required(false),
            string(CITY)
                .label("City")
                .description("The city where the contact lives.")
                .required(false),
            string(STATE)
                .label("State")
                .description("The state where the contact lives.")
                .required(false),
            string(ZIP_CODE)
                .label("Zip Code")
                .description("The zip code of the contact.")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("The country where the contact lives.")
                .required(false),
            string(WEBSITE)
                .label("Website")
                .description("The website of the contact.")
                .required(false),
            string(PHONE)
                .label("Phone")
                .description("The phone number of the contact.")
                .required(false),
            string(COMPANY)
                .label("Company")
                .description("The company where the contact works.")
                .required(false),
            array(TAGS)
                .label("Tags")
                .description("Tags of the contact.")
                .items(string("tag"))
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        number("id")
                            .description("The id of the contact."),
                        string("type")
                            .description("The type of the contact."),
                        integer("created_time")
                            .description("The time the contact was created."),
                        integer("updated_time")
                            .description("The time the contact was updated."),
                        integer("last_contacted")
                            .description("The time the contact was last contacted."),
                        integer("last_emailed")
                            .description("The time the contact was last emailed."),
                        integer("last_campaign_emailed")
                            .description("The time the contact was last emailed about the campaign."),
                        integer("last_called")
                            .description("The time the contact was last called."),
                        integer("viewed_time")
                            .description("The time the contact was last viewed."),
                        object("viewed")
                            .properties(
                                integer("viewed_time")
                                    .description("The time the contact was last viewed.")),
                        integer("star_value")
                            .description("The star value of the contact."),
                        integer("lead_score")
                            .description("The lead score of the contact."),
                        string("klout_score")
                            .description("The klout score of the contact."),
                        array("tags")
                            .items(string())
                            .description("Tags of the contact."),
                        array("tagsWithTime")
                            .items(
                                object()
                                    .properties(
                                        string("tag")
                                            .description("The tag name."),
                                        number("createdTime")
                                            .description("The time the tag was created."),
                                        integer("availableCount")
                                            .description("The available count of the tag."),
                                        string("entity_type")
                                            .description("The entity type."))),
                        array("properties")
                            .description("Properties of the contact.")
                            .items(
                                object()
                                    .properties(
                                        string("type")
                                            .description("The type of the contact."),
                                        string("name")
                                            .description("The name of the contact."),
                                        string("value")
                                            .description("The value of the contact."))),
                        array("campaignStatus")
                            .description("The status of the campaign."),
                        string("entity_type")
                            .description("The entity type."),
                        string("source")
                            .description("The source that created the contact."),
                        string("contact_company_id")
                            .description("The company ID of the contact."),
                        array("unsubscribeStatus"),
                        array("emailBounceStatus"),
                        integer("formId")
                            .description("The form ID of the contact."),
                        array("browserId")
                            .description("The browser ID of the contact."),
                        integer("lead_source_id")
                            .description("The lead source ID of the contact."),
                        integer("lead_status_id")
                            .description("The lead status ID of the contact."),
                        bool("is_lead_converted")
                            .description("Whether the lead converted the contact."),
                        integer("lead_converted_time")
                            .description("The time when the lead converted the contact."),
                        bool("is_duplicate_existed")
                            .description("Whether the duplicate of the contact exists ."),
                        integer("trashed_time")
                            .description("The time when the contact was trashed."),
                        integer("restored_time")
                            .description("The time when the contact was restored."),
                        bool("is_duplicate_verification_failed")
                            .description("Whether the duplicate of the contact verification failed."),
                        bool("is_client_import")
                            .description("Whether the contact was imported."),
                        bool("concurrent_save_allowed")
                            .description("Whether the contact was saved as concurrent."),
                        object("owner")
                            .description("The owner of the contact.")
                            .properties(
                                number("id")
                                    .description("The ID of the owner."),
                                string("domain")
                                    .description("The domain of the owner."),
                                string("email")
                                    .description("The email of the owner."),
                                string("phone")
                                    .description("The phone number of the owner."),
                                string("name")
                                    .description("The name of the owner."),
                                string("pic")
                                    .description("The picture of the owner."),
                                string("schedule_id")
                                    .description("The schedule ID of the owner."),
                                string("calendar_url")
                                    .description("The calendar URL of the owner."),
                                string("calendarURL")
                                    .description("The calendar URL of the owner.")))))
        .perform(AgileCrmCreateContactAction::perform);

    private AgileCrmCreateContactAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/contacts"))
            .body(
                Body.of(
                    TAGS, inputParameters.getList(TAGS, String.class),
                    PROPERTIES, getPropertiesList(inputParameters)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
