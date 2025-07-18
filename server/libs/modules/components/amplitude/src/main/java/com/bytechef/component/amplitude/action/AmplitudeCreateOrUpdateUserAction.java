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
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CARRIER;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CITY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CONTENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CONTENT_TYPE_URLENCODED;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.COUNTRY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.DEVICE_BRAND;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.DEVICE_ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.DMA;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFICATION;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.LANGUAGE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.OS_NAME;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.PLATFORM;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES_OBJECT;
import static com.bytechef.component.amplitude.util.AmplitudeUtils.getIdentification;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AmplitudeCreateOrUpdateUserAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrUpdateUser")
        .title("Create or Update User")
        .description("Creates or updates user without sending an event.")
        .properties(
            string(ID)
                .label("ID")
                .description("Choose to create or update a user or a device.")
                .options(
                    option("User ID", USER_ID),
                    option("Device ID", DEVICE_ID))
                .required(true),
            string(DEVICE_ID)
                .label("Device ID")
                .description("A device specific identifier, such as the Identifier for Vendor (IDFV) on iOS.")
                .displayCondition("%s == '%s'".formatted(ID, DEVICE_ID))
                .required(true),
            string(USER_ID)
                .label("User ID")
                .description(
                    "Unique user ID specified by you. If you send a request with a user ID that's not in the " +
                        "Amplitude system, new user will be created (e.g. email address).")
                .displayCondition("%s == '%s'".formatted(ID, USER_ID))
                .required(true),
            USER_PROPERTIES_OBJECT,
            string(PLATFORM)
                .label("Platform")
                .description("The platform that's sending the data.")
                .required(false),
            string(OS_NAME)
                .label("Operating System Name")
                .description("The mobile operating system or browser the user is on.")
                .required(false),
            string(DEVICE_BRAND)
                .label("Device Brand")
                .description("The device brand the user is on.")
                .required(false),
            string(CARRIER)
                .label("Carrier")
                .description("The carrier of the device the user is on.")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("The country the user is in.")
                .required(false),
            string(CITY)
                .label("City")
                .description("The city the user is in.")
                .required(false),
            string(DMA)
                .label("Designated Market Area ")
                .description("The Designated Market Area of the user.")
                .required(false),
            string(LANGUAGE)
                .label("Language")
                .description("The language the user has set.")
                .required(false))
        .output(
            outputSchema(
                string()
                    .description("Response message")))
        .perform(AmplitudeCreateOrUpdateUserAction::perform);

    private AmplitudeCreateOrUpdateUserAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> identification = getIdentification(inputParameters);

        Map<String, Object> body = new HashMap<>();

        body.put(API_KEY, connectionParameters.getRequiredString(API_KEY));
        body.put(IDENTIFICATION, context.json(json -> json.write(identification)));

        checkIfNull(body, CARRIER, inputParameters.getString(CARRIER));
        checkIfNull(body, COUNTRY, inputParameters.getString(COUNTRY));
        checkIfNull(body, CITY, inputParameters.getString(CITY));
        checkIfNull(body, DEVICE_BRAND, inputParameters.getString(DEVICE_BRAND));
        checkIfNull(body, DMA, inputParameters.getString(DMA));
        checkIfNull(body, LANGUAGE, inputParameters.getString(LANGUAGE));
        checkIfNull(body, OS_NAME, inputParameters.getString(OS_NAME));

        return context.http(http -> http.post("/identify"))
            .configuration(responseType(ResponseType.TEXT))
            .header(CONTENT_TYPE, CONTENT_TYPE_URLENCODED)
            .body(Body.of(body, BodyContentType.FORM_URL_ENCODED))
            .execute()
            .getBody(String.class);
    }

    private static void checkIfNull(Map<String, Object> body, String key, String value) {
        if (value != null) {
            body.put(key, value);
        }
    }
}
