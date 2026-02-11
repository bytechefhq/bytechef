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

package com.bytechef.component.pipedrive.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.pipedrive.constant.PipedriveConstants.ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 * @author Monika Kušter
 */
public class PipedriveUtils extends AbstractPipedriveUtils {

    private PipedriveUtils() {
    }

    public static List<Option<String>> getCurrencyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getStringOptions(context, "/currencies");
    }

    public static List<Option<Long>> getDealIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/deals");
    }

    public static List<Option<Long>> getFilterIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/filters");
    }

    public static List<Option<String>> getLabelIdsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getStringOptions(context, "/dealLabels");
    }

    public static List<Option<String>> getLeadIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getStringOptions(context, "/lead");
    }

    public static List<Option<Long>> getOrgIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/organizations");
    }

    public static List<Option<Long>> getOrganizationIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/organizations");
    }

    public static List<Option<Long>> getOwnerIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/users");
    }

    public static List<Option<Long>> getPersonIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/persons");
    }

    public static List<Option<Long>> getPipelineIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/pipelines");
    }

    public static List<Option<Long>> getStageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/stages");
    }

    public static List<Option<Long>> getUserIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        return getLongOptions(context, "/users");
    }

    private static List<Option<String>> getStringOptions(Context context, String path) {
        return getOptions(context, path, (map, pathKey) -> {
            String name = (String) map.get("name");

            if (pathKey.equals("/currencies")) {
                return option(name, (String) map.get("symbol"));
            } else {
                return option(name, (String) map.get(ID));
            }
        });
    }

    private static List<Option<Long>> getLongOptions(Context context, String path) {
        return getOptions(context, path, (map, pathKey) -> {
            int id = (Integer) map.get(ID);
            String name = (String) map.get("name");

            if (pathKey.equals("/deals")) {
                return option((String) map.get("title"), id);
            } else {
                return option(name, id);
            }
        });
    }

    private static <T> List<Option<T>> getOptions(
        Context context, String path, BiFunction<Map<?, ?>, String, Option<T>> optionMapper) {

        Map<String, ?> response = context
            .http(http -> http.get(path))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        context.log(log -> log.debug("Response for path='%s': %s".formatted(path, response)));

        List<Option<T>> options = new ArrayList<>();

        if (response.get("data") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(optionMapper.apply(map, path));
                }
            }
        }

        return options;
    }

    public static Integer subscribeWebhook(
        String eventObject, String eventAction, String webhookUrl, TriggerContext context) {

        Map<?, ?> result = context
            .http(http -> http.post("/webhooks"))
            .body(
                Http.Body.of(
                    "event_object", eventObject,
                    "event_action", eventAction,
                    "subscription_url", webhookUrl))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (result.get("data") instanceof Map<?, ?> map) {
            return (Integer) map.get(ID);
        }

        throw new ProviderException("Failed to start Pipedrive webhook.");
    }

    public static void unsubscribeWebhook(Integer webhookId, TriggerContext context) {
        context
            .http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }
}
