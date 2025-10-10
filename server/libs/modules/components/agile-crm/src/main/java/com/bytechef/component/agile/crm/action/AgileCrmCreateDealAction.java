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

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.DESCRIPTION;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.EXPECTED_VALUE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.MILESTONE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.OWNER_ID;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PIPELINE_ID;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PROBABILITY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.agile.crm.util.AgileCrmUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
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
public class AgileCrmCreateDealAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("crateDeal")
        .title("Create Deal")
        .description("Creates a new deal.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the deal.")
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Brief description about deal.")
                .required(false),
            number(EXPECTED_VALUE)
                .label("Expected Value")
                .description("Estimated value of a deal.")
                .required(true),
            number(PIPELINE_ID)
                .label("Pipeline ID")
                .description("ID of the pipeline that the deal follows.")
                .options((OptionsFunction<Long>) AgileCrmUtils::getPipelineIdOptions)
                .required(true),
            string(MILESTONE)
                .label("Milestone")
                .description("Milestone the deal is currently at.")
                .optionsLookupDependsOn(PIPELINE_ID)
                .options((OptionsFunction<String>) AgileCrmUtils::getMilestoneOptions)
                .required(true),
            integer(PROBABILITY)
                .label("Probability")
                .description("Should be ranging between 0-100.")
                .maxValue(100)
                .minValue(0)
                .required(true),
            string(OWNER_ID)
                .label("Owner ID")
                .description("ID of the owner of the deal.")
                .options((OptionsFunction<String>) AgileCrmUtils::getUserIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("colorName")
                            .description("Color of the deal in display."),
                        number("id")
                            .description("ID of the deal."),
                        bool("apply_discount")
                            .description("Whether the discount is applied."),
                        number("discount_value")
                            .description("The discount value of the deal."),
                        number("discount_amt")
                            .description("The discount amount of the deal."),
                        string("discount_type")
                            .description("The discount type of the deal."),
                        string("name")
                            .description("The name of the deal."),
                        array("contact_ids")
                            .description("The contacts that are part of the deal."),
                        array("custom_data")
                            .description("Custom data of the deal."),
                        array("products")
                            .description("The products that are part of the deal."),
                        string("description")
                            .description("The description of the deal."),
                        number("expected_value")
                            .description("The expected value of the deal."),
                        string("milestone")
                            .description("The milestone of the deal."),
                        number("probability")
                            .description("The probability of the deal being won."),
                        string("owner_id")
                            .description("The ID of the owner of the deal."),
                        integer("created_time")
                            .description("The time when the deal was created."),
                        integer("milestone_changed_time")
                            .description("The time when the deal milestone was changed."),
                        string("entity_type")
                            .description("The entity type."),
                        array("notes")
                            .description("The notes of the deal."),
                        array("notes_ids")
                            .description("The notes ID."),
                        integer("note_created_time")
                            .description("The time when the deal note was created."),
                        number("pipeline_id")
                            .description("The ID of the pipeline."),
                        bool("archived")
                            .description("Whether the deal is archived."),
                        integer("won_date")
                            .description("The date when the deal was won."),
                        integer("lost_reason_id")
                            .description("The ID of the lost reason."),
                        integer("deal_source_id")
                            .description("The ID of the deal source."),
                        number("total_deal_value")
                            .description("The total value of the deal."),
                        integer("updated_time")
                            .description("The time when the deal was updated."),
                        bool("isCurrencyUpdateRequired")
                            .description("Whether the deal currency requires updating."),
                        number("currency_conversion_value")
                            .description("The currency conversion value."),
                        array("tags")
                            .items(string())
                            .description("Tags of the deal."),
                        array("tagsWithTime")
                            .description("Tags with time of the deal."),
                        array("owner")
                            .description("The owner of the deal.")
                            .items(
                                object()
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
                                            .description("The calendar URL of the owner."))),
                        array("contacts")
                            .description("Contacts of the deal."))))
        .perform(AgileCrmCreateDealAction::perform);

    private AgileCrmCreateDealAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/opportunity"))
            .body(
                Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    EXPECTED_VALUE, inputParameters.getRequiredDouble(EXPECTED_VALUE),
                    MILESTONE, inputParameters.getRequiredString(MILESTONE),
                    PROBABILITY, String.valueOf(inputParameters.getRequiredInteger(PROBABILITY)),
                    OWNER_ID, inputParameters.getRequiredString(OWNER_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
