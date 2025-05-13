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

package com.bytechef.component.attio.util;

import static com.bytechef.component.attio.constant.AttioConstants.COMPANIES;
import static com.bytechef.component.attio.constant.AttioConstants.COMPANY_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEALS;
import static com.bytechef.component.attio.constant.AttioConstants.DEAL_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static com.bytechef.component.attio.constant.AttioConstants.PEOPLE;
import static com.bytechef.component.attio.constant.AttioConstants.PERSON_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.TARGET_OBJECT;
import static com.bytechef.component.attio.constant.AttioConstants.USERS;
import static com.bytechef.component.attio.constant.AttioConstants.USER_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACES;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACE_RECORD;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Nikolina Spehar
 */
public class AttioUtils {

    public static ActionOptionsFunction<String> getCompanyIdOptions(String attribute) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            Map<String, List<Map<String, Object>>> attributes = context.http(
                http -> http.get("/objects/companies/attributes/%s/options".formatted(attribute)))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> options = new ArrayList<>();

            for (Map<String, Object> attributeMap : attributes.get(DATA)) {
                if (attributeMap.get(ID) instanceof Map<?, ?> idMap) {
                    options.add(
                        option((String) attributeMap.get("title"), (String) idMap.get("option_id")));
                }
            }

            return options;
        };
    }

    public static Object getContent(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("events");
    }

    public static List<Option<String>> getDealStageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> stages = context.http(
            http -> http.get("/objects/deals/attributes/stage/statuses"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> stage : stages.get(DATA)) {
            if (stage.get(ID) instanceof Map<?, ?> stageId) {
                options.add(option((String) stage.get("title"), (String) stageId.get("status_id")));
            }
        }

        return options;
    }

    public static List<ModifiableValueProperty<?, ?>> getRecordAttributes(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        Context context) {

        return switch (inputParameters.getRequiredString(RECORD_TYPE)) {
            case USERS -> USER_RECORD;
            case WORKSPACES -> WORKSPACE_RECORD;
            case COMPANIES -> COMPANY_RECORD;
            case PEOPLE -> PERSON_RECORD;
            case DEALS -> DEAL_RECORD;
            default -> List.of();
        };
    }

    public static List<Option<String>> getTargetActorIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> actors = context.http(
            http -> http.post("/objects/users/records/query"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> actordMap : actors.get(DATA)) {
            if (actordMap.get(ID) instanceof Map<?, ?> actorId &&
                actordMap.get("values") instanceof Map<?, ?> actorValues &&
                actorValues.get("primary_email_address") instanceof List<?> actorValueEmailList &&
                actorValueEmailList.getFirst() instanceof Map<?, ?> actorValueEmail) {

                options.add(
                    option((String) actorValueEmail.get("email_address"), (String) actorId.get("record_id")));
            }
        }

        return options;
    }

    public static List<Option<String>> getTargetObjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> objects = context.http(http -> http.get("/objects"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> object : objects.get(DATA)) {
            options.add(option((String) object.get("singular_noun"), (String) object.get("api_slug")));
        }

        return options;
    }

    public static ActionOptionsFunction<String> getTargetRecordIdOptions(String targetObject) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            String object;
            if (Objects.equals(targetObject, RECORD_TYPE)) {
                object = inputParameters.getRequiredString(RECORD_TYPE);
            } else if (Objects.equals(targetObject, TARGET_OBJECT)) {
                object = inputParameters.getRequiredString(TARGET_OBJECT);
            } else {
                object = targetObject;
            }

            Map<String, List<Object>> records = context.http(
                http -> http.post("/objects/%s/records/query".formatted(object)))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            List<Option<String>> recordIdOptions = new ArrayList<>();

            for (Object record : records.get(DATA)) {
                if (record instanceof Map<?, ?> recordMap &&
                    recordMap.get(ID) instanceof Map<?, ?> recordId &&
                    recordMap.get("values") instanceof Map<?, ?> recordValues) {

                    String recordName = null;

                    if (recordValues.get("name") instanceof List<?> recordValueNameList &&
                        !recordValueNameList.isEmpty() &&
                        recordValueNameList.getFirst() instanceof Map<?, ?> recordValueName) {
                        if (object.equals("people")) {
                            recordName = (String) recordValueName.get("full_name");
                        } else {
                            recordName = (String) recordValueName.get("value");
                        }
                    }

                    recordIdOptions.add(option(recordName, (String) recordId.get("record_id")));
                }
            }

            return recordIdOptions;
        };
    }

    public static List<Option<String>> getWorkSpaceMemberIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, List<Map<String, Object>>> actors = context.http(http -> http.get("/workspace_members"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> actor : actors.get(DATA)) {
            if (actor.get(ID) instanceof Map<?, ?> actorId) {
                String fullName = actor.get("first_name") + " " + actor.get("last_name");

                options.add(option(fullName, (String) actorId.get("workspace_member_id")));
            }
        }

        return options;
    }

    public static String subscribeWebhook(String eventType, Context context, String webhookUrl) {
        Map<String, Map<String, Object>> body = context.http(http -> http.post("/webhooks"))
            .body(
                Body.of(
                    DATA, Map.of(
                        "target_url", webhookUrl,
                        "subscriptions",
                        List.of(Map.of("event_type", eventType, "filter", Map.of("$and", List.of()))))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> data = body.get(DATA);

        if (data.get(ID) instanceof Map<?, ?> map) {
            return (String) map.get("webhook_id");
        }

        throw new ProviderException("Failed to subscribe to webhook");
    }

    public static void unsubscribeWebhook(Context context, String webhookId) {
        context.http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .execute();
    }
}
