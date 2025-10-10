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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.BODY;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.IS_DRAFT;
import static com.bytechef.component.vbout.constant.VboutConstants.IS_SCHEDULED;
import static com.bytechef.component.vbout.constant.VboutConstants.LISTS;
import static com.bytechef.component.vbout.constant.VboutConstants.NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.REPLY_TO;
import static com.bytechef.component.vbout.constant.VboutConstants.SCHEDULED_DATETIME;
import static com.bytechef.component.vbout.constant.VboutConstants.SUBJECT;
import static com.bytechef.component.vbout.constant.VboutConstants.TYPE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.vbout.util.VboutUtils;

/**
 * @author Marija Horvat
 */
public class VboutCreateEmailMarketingCampaignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEmailMarketingCampaign")
        .title("Create Email Marketing Campaign")
        .description("Creates a new email campaign for specific list.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the campaign.")
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("The subject line for the campaign.")
                .required(true),
            string(FROM_EMAIL)
                .label("From Mail")
                .description("The from email of the campaign.")
                .required(true),
            string(FROM_NAME)
                .label("From Name")
                .description("The from name of the campaign.")
                .required(true),
            string(REPLY_TO)
                .label("Reply To")
                .description("The reply to email of the campaign.")
                .required(true),
            string(BODY)
                .label("Body")
                .description("Message body.")
                .required(true),
            string(TYPE)
                .label("Type")
                .description("The type of the campaign.")
                .options(
                    option("Standard", "standard"),
                    option("Automated", "automated"))
                .required(false),
            bool(IS_SCHEDULED)
                .label("Is Scheduled")
                .description("The flag to schedule the campaign for the future.")
                .required(false),
            date(SCHEDULED_DATETIME)
                .label("Scheduled Date")
                .description("The date to schedule the campaign.")
                .displayCondition("%s == true".formatted(IS_SCHEDULED))
                .required(false),
            bool(IS_DRAFT)
                .label("Is Draft")
                .description("The flag to set the campaign to draft.")
                .required(false),
            array(LISTS)
                .label("Lists")
                .description("IDs of list campaign recipients.")
                .items(string())
                .options((OptionsFunction<String>) VboutUtils::getListIdOptions)
                .required(false))
        .perform(VboutCreateEmailMarketingCampaignAction::perform);

    private VboutCreateEmailMarketingCampaignAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/emailMarketing/AddCampaign"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                NAME, inputParameters.getRequiredString(NAME),
                SUBJECT, inputParameters.getRequiredString(SUBJECT),
                FROM_EMAIL, inputParameters.getRequiredString(FROM_EMAIL),
                FROM_NAME, inputParameters.getRequiredString(FROM_NAME),
                REPLY_TO, inputParameters.getRequiredString(REPLY_TO),
                BODY, inputParameters.getRequiredString(BODY),
                TYPE, inputParameters.getString(TYPE),
                IS_SCHEDULED, inputParameters.getBoolean(IS_SCHEDULED),
                SCHEDULED_DATETIME, inputParameters.getString(SCHEDULED_DATETIME),
                IS_DRAFT, inputParameters.getBoolean(IS_DRAFT),
                LISTS, inputParameters.getArray(LISTS))
            .execute();

        return null;
    }
}
