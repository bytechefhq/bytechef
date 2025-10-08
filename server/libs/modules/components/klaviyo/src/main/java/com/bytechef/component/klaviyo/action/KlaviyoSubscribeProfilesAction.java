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
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ATTRIBUTES;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.DATA;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.EMAIL;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PHONE_NUMBER;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.SUBSCRIPTION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TYPE;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfileEmail;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfilePhoneNumber;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.klaviyo.util.KlaviyoUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Marija Horvat
 */
public class KlaviyoSubscribeProfilesAction {

    enum SubscriptionType {
        EMAIL("email"),
        SMS("sms");

        private final String value;

        SubscriptionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("subscribeProfiles")
        .title("Subscribe Profiles")
        .description("Subscribe one or more profiles to email marketing, SMS marketing, or both.")
        .properties(
            array(PROFILE_ID)
                .label("Profile ID")
                .description("The IDs of the profile to subscribe.")
                .items(string())
                .options((OptionsFunction<String>) KlaviyoUtils::getProfileIdOptions)
                .required(true),
            array(SUBSCRIPTION)
                .label("Subscription")
                .description("The subscription parameters to subscribe to on the email or sms channel.")
                .items(string())
                .options(
                    List.of(
                        option("EMAIL", SubscriptionType.EMAIL.getValue()),
                        option("SMS", SubscriptionType.SMS.getValue())))
                .required(true))
        .perform(KlaviyoSubscribeProfilesAction::perform);

    private KlaviyoSubscribeProfilesAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, Object>> data = getData(inputParameters, context);

        context.http(http -> http.post("/api/profile-subscription-bulk-create-jobs"))
            .body(
                Body.of(
                    DATA, Map.of(
                        TYPE, "profile-subscription-bulk-create-job",
                        ATTRIBUTES, Map.of("profiles", Map.of(DATA, data)))))
            .configuration(responseType(ResponseType.JSON))
            .execute();

        return null;
    }

    private static List<Map<String, Object>> getData(Parameters inputParameters, Context context) {
        List<String> profileIds = inputParameters.getRequiredList(PROFILE_ID, String.class);
        List<String> subscriptionTypes = inputParameters.getRequiredList(SUBSCRIPTION, String.class);

        List<Map<String, Object>> profilesData = new ArrayList<>();

        for (String profileId : profileIds) {
            Map<String, Object> subscriptions = new HashMap<>();
            Map<String, Object> attributes = new HashMap<>();

            addSubscription(subscriptionTypes, subscriptions, context, profileId, SubscriptionType.EMAIL, attributes);
            addSubscription(subscriptionTypes, subscriptions, context, profileId, SubscriptionType.SMS, attributes);

            attributes.put("subscriptions", subscriptions);

            profilesData.add(createProfileData(profileId, attributes));
        }

        return profilesData;
    }

    private static void addSubscription(
        List<String> subscriptionTypes, Map<String, Object> subscriptions, Context context, String profileId,
        SubscriptionType subscriptionType, Map<String, Object> attributes) {

        if (subscriptionTypes.contains(subscriptionType.getValue())) {
            Map<String, Object> subscription = Map.of("marketing", Map.of("consent", "SUBSCRIBED"));

            subscriptions.put(subscriptionType.getValue(), subscription);

            String value = subscriptionType == SubscriptionType.EMAIL ? getProfileEmail(context, profileId)
                : getProfilePhoneNumber(context, profileId);

            Optional.ofNullable(value)
                .filter(v -> !v.isEmpty())
                .ifPresent(v -> attributes.put(subscriptionType == SubscriptionType.EMAIL ? EMAIL : PHONE_NUMBER, v));
        }
    }

    private static Map<String, Object> createProfileData(String profileId, Map<String, Object> attributes) {
        return Map.of(TYPE, PROFILE, ID, profileId, ATTRIBUTES, attributes);
    }
}
