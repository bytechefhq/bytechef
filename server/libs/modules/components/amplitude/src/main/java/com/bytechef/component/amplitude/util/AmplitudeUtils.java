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

package com.bytechef.component.amplitude.util;

import static com.bytechef.component.amplitude.constant.AmplitudeConstants.EVENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFIER;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.PLATFORM;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AmplitudeUtils {

    public static String getEventJson(Parameters inputParameters, Context context) {
        Map<String, String> identifier = inputParameters.getRequiredMap(IDENTIFIER, String.class);

        Map<String, Object> event = Map.of(
            EVENT_TYPE, inputParameters.getRequiredString(EVENT_TYPE),
            PLATFORM, inputParameters.getRequiredString(PLATFORM),
            identifier.get(KEY), identifier.get(VALUE),
            USER_PROPERTIES, getUserProperties(inputParameters));

        return context.json(json -> json.write(event));
    }

    public static Map<String, Object> getIdentification(Parameters parameters) {
        Map<String, Object> identification = new HashMap<>();

        String id = parameters.getRequiredString(ID);
        identification.put(id, parameters.getRequiredString(id));

        identification.put(USER_PROPERTIES, getUserProperties(parameters));

        return identification;
    }

    public static List<Option<String>> getIdentifierKeyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        String platform = inputParameters.getRequiredString(PLATFORM);

        if (platform.equals("ios")) {
            options.add(option("The Identifier for Advertiser", "idfa"));
            options.add(option("The Identifier for Vendor", "idfv"));
        } else {
            options.add(option("The Google ADID", "adid"));
            options.add(option("App Set ID", "android_app_set_id"));
        }

        return options;
    }

    public static Map<String, String> getUserProperties(Parameters inputParameters) {
        Map<String, String> userProperties = new HashMap<>();

        List<Map<String, String>> userPropertiesList = inputParameters.getList(
            USER_PROPERTIES, new TypeReference<>() {}, List.of());

        for (Map<String, String> userProperty : userPropertiesList) {
            userProperties.put(userProperty.get(KEY), userProperty.get(VALUE));
        }

        return userProperties;
    }
}
