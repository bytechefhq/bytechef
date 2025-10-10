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

package com.bytechef.component.amplitude.action;

import static com.bytechef.component.amplitude.constant.AmplitudeConstants.API_KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.EVENT;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.EVENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFIER;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.PLATFORM;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES_OBJECT;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.VALUE;
import static com.bytechef.component.amplitude.util.AmplitudeUtils.getEventJson;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.amplitude.util.AmplitudeUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class AmplitudeCreateAttributionEventAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createAttributionEvent")
        .title("Create Attribution Event")
        .description("Creates attribution event using Attribution API.")
        .properties(
            string(EVENT_TYPE)
                .label("Event Type")
                .description("The event info. Prefix with brackets [YOUR COMPANY].")
                .required(true),
            string(PLATFORM)
                .label("Platform")
                .description("Platform which the event  will occur on.")
                .options(
                    option("iOS", "ios"),
                    option("Android", "android"))
                .required(true),
            object(IDENTIFIER)
                .label("Identifier")
                .description("Identifier of the platform.")
                .required(true)
                .properties(
                    string(KEY)
                        .label("Identifier Key")
                        .description(
                            "For iOS input the Identifier for Advertiser or the Identifier for Vendor.For Android " +
                                "input the Google ADID or App Set ID.")
                        .options((OptionsFunction<String>) AmplitudeUtils::getIdentifierKeyOptions)
                        .optionsLookupDependsOn(PLATFORM)
                        .required(true),
                    string(VALUE)
                        .label("Identifier Value")
                        .description("Value of selected identifier.")
                        .required(true)),
            USER_PROPERTIES_OBJECT)
        .output(
            outputSchema(
                string()
                    .description("Response message")))
        .perform(AmplitudeCreateAttributionEventAction::perform);

    private AmplitudeCreateAttributionEventAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/attribution"))
            .configuration(responseType(ResponseType.TEXT))
            .queryParameters(
                API_KEY, connectionParameters.getRequiredString(API_KEY),
                EVENT, getEventJson(inputParameters, context))
            .execute()
            .getBody(String.class);
    }
}
