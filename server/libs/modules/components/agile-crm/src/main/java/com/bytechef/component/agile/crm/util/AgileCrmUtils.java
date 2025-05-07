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

package com.bytechef.component.agile.crm.util;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.VALUE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.WEBSITE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmUtils {

    public static String ifPropertyIsNull(String property) {
        return property == null ? "" : property;
    }

    public static List<Map<String, Object>> getPropertiesList(Parameters properties) {

        List<Map<String, Object>> propertiesList = new ArrayList<>();

        for (PropertiesValuesEnum value : PropertiesValuesEnum.values()) {
            Map<String, Object> propertiesMap = new HashMap<>();

            propertiesMap.put(NAME, value.getValue());
            propertiesMap.put(VALUE, ifPropertyIsNull(properties.getString(value.getValue())));

            if (value.getValue()
                .equals(WEBSITE)) {
                propertiesMap.put("subtype", "URL");
            }

            propertiesList.add(propertiesMap);
        }

        return propertiesList;
    }

    public static List<Option<String>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Map<String, Object>> userOptions = context.http(http -> http.get("/users"))
            .configuration(responseType(ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> user = userOptions.get("domainUser");

        return List.of(option((String) user.get("domain"), (String) user.get("id")));
    }

    private static List<Map<String, Object>> getTrackList(Context context) {
        return context.http(http -> http.get("/milestone/pipelines"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<Long>> getPipelineIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> trackOptions = getTrackList(context);

        List<Option<Long>> trackIdOptions = new ArrayList<>();

        for (Map<String, Object> trackOption : trackOptions) {
            trackIdOptions.add(option((String) trackOption.get("name"), Long.parseLong(trackOption.get("id")
                .toString())));
        }

        return trackIdOptions;
    }

    public static List<Option<String>> getMilestone(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> trackOptions = getTrackList(context);

        List<Option<String>> milestoneOptions = new ArrayList<>();

        String milestonesString = trackOptions.stream()
            .filter(
                trackOption -> Long.parseLong(trackOption.get("id")
                    .toString()) == inputParameters.getLong("pipeline_id"))
            .map(trackOption -> (String) trackOption.get("milestones"))
            .collect(Collectors.joining());

        for (String milestoneOption : milestonesString.split(",")) {
            milestoneOptions.add(option(milestoneOption, milestoneOption));
        }

        return milestoneOptions;
    }

    public static List<Option<String>> getTaskTypeOptions() {
        return List.of(
            option("Call", "CALL"),
            option("Email", "EMAIL"),
            option("Follow Up", "FOLLOW_UP"),
            option("Meeting", "MEETING"),
            option("Milestone", "MILESTONE"),
            option("Send", "SEND"),
            option("Tweet", "TWEET"),
            option("Other", "OTHER"));
    }
}
