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

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.ID;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PIPELINE_ID;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmUtils {

    public static List<Option<String>> getMilestoneOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> tracks = getTrackList(context);

        String milestones = tracks.stream()
            .filter(track -> (Long) track.get(ID) == inputParameters.getRequiredLong(PIPELINE_ID))
            .map(trackOption -> (String) trackOption.get("milestones"))
            .collect(Collectors.joining());

        return Arrays.stream(milestones.split(","))
            .map(milestone -> option(milestone, milestone))
            .collect(Collectors.toList());
    }

    public static List<Option<Long>> getPipelineIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, Object>> tracks = getTrackList(context);

        List<Option<Long>> options = new ArrayList<>();

        for (Map<String, Object> track : tracks) {
            options.add(option((String) track.get("name"), ((Long) track.get(ID)).longValue()));
        }

        return options;
    }

    public static List<Map<String, Object>> getPropertiesList(Parameters inputParameters) {
        return Arrays.stream(PropertiesValuesEnum.values())
            .map(propertiesValuesEnum -> createPropertiesMap(propertiesValuesEnum, inputParameters))
            .collect(Collectors.toList());
    }

    private static Map<String, Object> createPropertiesMap(
        PropertiesValuesEnum propertiesValuesEnum, Parameters inputParameters) {

        String value = propertiesValuesEnum.getValue();

        Map<String, Object> propertiesMap = new HashMap<>();

        propertiesMap.put(NAME, value);
        propertiesMap.put(VALUE, defaultIfNull(inputParameters.getString(value)));

        if (value.equals(WEBSITE)) {
            propertiesMap.put("subtype", "URL");
        }

        return propertiesMap;
    }

    private static String defaultIfNull(String property) {
        return property == null ? "" : property;
    }

    public static List<Option<String>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Map<String, Object>> userOptions = context.http(http -> http.get("/users"))
            .configuration(responseType(ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> user = userOptions.get("domainUser");

        return List.of(option((String) user.get("domain"), (String) user.get(ID)));
    }

    private static List<Map<String, Object>> getTrackList(Context context) {
        return context.http(http -> http.get("/milestone/pipelines"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
