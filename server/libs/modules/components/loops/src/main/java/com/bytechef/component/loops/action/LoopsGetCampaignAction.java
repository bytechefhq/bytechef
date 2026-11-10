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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.loops.constant.LoopsConstants.CAMPAIGN_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.CAMPAIGN_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.loops.util.LoopsUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LoopsGetCampaignAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getCampaign")
        .title("Get Campaign")
        .description("Get a campaign, including the ID of its email message.")
        .properties(
            string(CAMPAIGN_ID)
                .label("Campaign ID")
                .description("The ID of the campaign to get.")
                .options((OptionsFunction<String>) LoopsUtils::getCampaignOptions)
                .required(true))
        .output(outputSchema(CAMPAIGN_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/loops_v1#get-campaign")
        .perform(LoopsGetCampaignAction::perform);

    private LoopsGetCampaignAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.http(http -> http.get("/campaigns/" + inputParameters.getRequiredString(CAMPAIGN_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
