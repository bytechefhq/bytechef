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

package com.bytechef.component.loops.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.loops.constant.LoopsConstants.AUDIENCE_SEGMENT_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.CAMPAIGN_GROUP_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.CAMPAIGN_PROPERTIES;
import static com.bytechef.component.loops.constant.LoopsConstants.MAILING_LIST_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.loops.util.LoopsUtils;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class LoopsCreateCampaignAction {

    private static final ModifiableObjectProperty CREATE_CAMPAIGN_OUTPUT_PROPERTY = object()
        .properties(
            Stream.concat(
                CAMPAIGN_PROPERTIES.stream(),
                Stream.of(
                    string("emailMessageContentRevisionId")
                        .description(
                            "The content revision of the new email message. Pass it as Expected Revision ID on " +
                                "the first Update Email Message.")))
                .toList());

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCampaign")
        .title("Create Campaign")
        .description(
            "Create a draft campaign. Loops creates an empty email message for it; set its subject and content " +
                "with Update Email Message.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The campaign name.")
                .required(true),
            string(CAMPAIGN_GROUP_ID)
                .label("Campaign Group ID")
                .description(
                    "The ID of the group to add this campaign to. Defaults to the team's Unsorted group when empty.")
                .required(false),
            string(MAILING_LIST_ID)
                .label("Mailing List ID")
                .description("The ID of the mailing list to send to.")
                .options((OptionsFunction<String>) LoopsUtils::getMailingListOptions)
                .required(false),
            string(AUDIENCE_SEGMENT_ID)
                .label("Audience Segment ID")
                .description("The ID of an audience segment to send to.")
                .required(false))
        .output(outputSchema(CREATE_CAMPAIGN_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/loops_v1#create-campaign")
        .perform(LoopsCreateCampaignAction::perform);

    private LoopsCreateCampaignAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.http(http -> http.post("/campaigns"))
            .body(
                Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    CAMPAIGN_GROUP_ID, inputParameters.getString(CAMPAIGN_GROUP_ID),
                    MAILING_LIST_ID, inputParameters.getString(MAILING_LIST_ID),
                    AUDIENCE_SEGMENT_ID, inputParameters.getString(AUDIENCE_SEGMENT_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
