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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.SUBSCRIPTION;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfileEmail;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfilePhoneNumber;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.klaviyo.util.KlaviyoUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class KlaviyoSubscribeProfilesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("subscribeProfiles")
        .title("Subscribe Profiles")
        .description("Subscribe one or more profiles to email marketing, SMS marketing, or both.")
        .properties(
            array(PROFILE_ID)
                .label("Profile ID")
                .description("The IDs of the profile to subscribe.")
                .items(string())
                .options((ActionOptionsFunction<String>) KlaviyoUtils::getProfileIdOptions)
                .required(true),
            array(SUBSCRIPTION)
                .label("Subscription")
                .description("The subscription parameters to subscribe to on the email or sms channel.")
                .items(string())
                .options(List.of(option("EMAIL", "email"), option("SMS", "sms")))
                .required(true))
        .perform(KlaviyoSubscribeProfilesAction::perform);

    private KlaviyoSubscribeProfilesAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> data = getData(inputParameters, context);

        return context
            .http(http -> http.post("/api/profile-subscription-bulk-create-jobs"))
            .body(
                Body.of(
                    "data", Map.of(
                        "type", "profile-subscription-bulk-create-job",
                        "attributes", Map.of("profiles", Map.of("data", data)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static List<Map<String, Object>> getData(Parameters inputParameters, Context context) {

        List<String> profileIds = inputParameters.getList(PROFILE_ID, String.class);
        List<String> subscriptionTypes = inputParameters.getList(SUBSCRIPTION, String.class);

        List<Map<String, Object>> profilesData = new ArrayList<>();

        for (String profileId : profileIds) {

            Map<String, Object> subscriptions = new HashMap<>();
            Map<String, Object> attributes = new HashMap<>();

            if (subscriptionTypes.contains("email")) {
                Map<String, Object> emailSubscription = new HashMap<>();
                emailSubscription.put("marketing", Map.of("consent", "SUBSCRIBED"));
                subscriptions.put("email", emailSubscription);

                String email = getProfileEmail(context, profileId);
                if (email != null && !email.isEmpty()) {
                    attributes.put("email", email);
                }
            }

            if (subscriptionTypes.contains("sms")) {
                Map<String, Object> smsSubscription = new HashMap<>();
                smsSubscription.put("marketing", Map.of("consent", "SUBSCRIBED"));
                subscriptions.put("sms", smsSubscription);

                String phoneNumber = getProfilePhoneNumber(context, profileId);
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    attributes.put("phone_number", phoneNumber);
                }
            }
            attributes.put("subscriptions", subscriptions);

            Map<String, Object> profileData = Map.of(
                "type", "profile",
                "id", profileId,
                "attributes", attributes);
            profilesData.add(profileData);
        }
        return profilesData;
    }
}
