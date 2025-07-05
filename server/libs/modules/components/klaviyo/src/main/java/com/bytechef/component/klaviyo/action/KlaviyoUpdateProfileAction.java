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

package com.bytechef.component.klaviyo.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ADDRESS1;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ADDRESS2;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.CITY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.COUNTRY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.EMAIL;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.FIRST_NAME;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.IMAGE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.IP;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LAST_NAME;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LOCALE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ORGANIZATION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PHONE_NUMBER;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_OUTPUT_PROPERTY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.REGION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TIMEZONE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TITLE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ZIP;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.klaviyo.util.KlaviyoUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marija Horvat
 */
public class KlaviyoUpdateProfileAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateProfile")
        .title("Update Profile")
        .description("Update the profile with the given profile ID.")
        .properties(
            string(PROFILE_ID)
                .label("Profile ID")
                .description("Primary key that uniquely identifies this profile.")
                .options((ActionOptionsFunction<String>) KlaviyoUtils::getProfileIdOptions)
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("Individual's email address.")
                .required(false),
            string(PHONE_NUMBER)
                .label("Phone Number")
                .description("Individual's phone number in E.164 format.")
                .required(false),
            string(FIRST_NAME)
                .label("First Name")
                .description("Individual's first name.")
                .required(false),
            string(LAST_NAME)
                .label("Last Name")
                .description("Individual's last name.")
                .required(false),
            string(ORGANIZATION)
                .label("Organization")
                .description("Name of the company or organization within the company for whom the individual works.")
                .required(false),
            string(LOCALE)
                .label("Locale")
                .description(
                    "The locale of the profile, in the IETF BCP 47 language tag format like (ISO 639-1/2)-(ISO 3166 alpha-2).")
                .required(false),
            string(TITLE)
                .label("Title")
                .description("Individual's job title.")
                .required(false),
            string(IMAGE)
                .label("Image")
                .description("URL pointing to the location of a profile image.")
                .required(false),
            string(ADDRESS1)
                .label("Address1")
                .description("First line of street address.")
                .required(false),
            string(ADDRESS2)
                .label("Address2")
                .description("Second line of street address.")
                .required(false),
            string(CITY)
                .label("City")
                .description("City name.")
                .required(false),
            string(COUNTRY)
                .label("Country")
                .description("Country name.")
                .required(false),
            string(REGION)
                .label("Region")
                .description("Region within a country, such as state or province.")
                .required(false),
            string(ZIP)
                .label("Zip")
                .description("Zip code.")
                .required(false),
            string(TIMEZONE)
                .label("Timezone")
                .description("Time zone name. We recommend using time zones from the IANA Time Zone Database..")
                .required(false),
            string(IP)
                .label("IP")
                .description("IP address.")
                .required(false))
        .output(outputSchema(PROFILE_OUTPUT_PROPERTY))
        .perform(KlaviyoUpdateProfileAction::perform);

    private KlaviyoUpdateProfileAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> attributes = getAttributes(inputParameters);

        return context
            .http(http -> http.patch("/api/profiles/" + inputParameters.getRequiredString(PROFILE_ID)))
            .headers(Map.of(
                "accept", List.of("application/vnd.api+json"),
                "revision", List.of("2025-04-15")))
            .body(
                Body.of(
                    "data", Map.of(
                        "type", "profile",
                        "id", inputParameters.getRequiredString(PROFILE_ID),
                        "attributes", attributes)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static Map<String, Object> getAttributes(Parameters inputParameters) {

        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> location = new HashMap<>();

        List<String> attributeKeys = List.of(
            EMAIL, PHONE_NUMBER, FIRST_NAME, LAST_NAME, ORGANIZATION, LOCALE, TITLE, IMAGE);

        List<String> locationKeys = List.of(ADDRESS1, ADDRESS2, CITY, COUNTRY, REGION, ZIP, TIMEZONE, IP);

        for (String key : attributeKeys) {
            addIfPresent(attributes, key, inputParameters);
        }

        for (String key : locationKeys) {
            addIfPresent(location, key, inputParameters);
        }

        if (!location.isEmpty()) {
            attributes.put("location", location);
        }

        return attributes;
    }

    private static void addIfPresent(Map<String, Object> map, String key, Parameters parameters) {
        Optional.ofNullable(parameters.getString(key))
            .filter(value -> !value.isBlank())
            .ifPresent(value -> map.put(key, value));
    }
}
