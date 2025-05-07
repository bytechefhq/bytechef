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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AttioUtils {

    public static List<Option<String>> getCompanyArrIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getCompanyIdOptions(context, "estimated_arr_usd");
    }

    public static List<Option<String>> getCompanyCategoriesIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getCompanyIdOptions(context, "categories");
    }

    public static List<Option<String>> getCompanyEmployeeRangeIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getCompanyIdOptions(context, "employee_range");
    }

    private static List<Option<String>> getCompanyIdOptions(Context context, String attribute) {
        Map<String, List<Object>> attributes = context.http(
            http -> http.get("/objects/companies/attributes/%s/options".formatted(attribute)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> attributeIdOptions = new ArrayList<>();

        for (Object object : attributes.get(DATA)) {
            if (object instanceof Map<?, ?> attributeMap
                && attributeMap.get("id") instanceof Map<?, ?> attributeId) {

                attributeIdOptions
                    .add(option((String) attributeMap.get("title"), (String) attributeId.get("option_id")));
            }
        }

        return attributeIdOptions;
    }

    public static Object getContent(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        return content.get("events");
    }

    public static List<Option<String>> getDealStageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, List<Object>> stages = context.http(
            http -> http.get("/objects/deals/attributes/stage/statuses"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> dealStageIdOptions = new ArrayList<>();

        for (Object stage : stages.get(DATA)) {
            if (stage instanceof Map<?, ?> stageMap
                && stageMap.get("id") instanceof Map<?, ?> stageId) {

                dealStageIdOptions
                    .add(option((String) stageMap.get("title"), (String) stageId.get("status_id")));
            }
        }

        return dealStageIdOptions;
    }

    public static List<ModifiableValueProperty<?, ?>> getRecordAttributes(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        Context context) {

        switch (inputParameters.getRequiredString(RECORD_TYPE)) {
            case USERS:
                return List.of(USER_RECORD);
            case WORKSPACES:
                return List.of(WORKSPACE_RECORD);
            case COMPANIES:
                return List.of(COMPANY_RECORD);
            case PEOPLE:
                return List.of(PERSON_RECORD);
            case DEALS:
                return List.of(DEAL_RECORD);
            default:
                return List.of();
        }

    }

    public static List<Option<String>> getTargetActorIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, List<Object>> actors = context.http(
            http -> http.post("/objects/users/records/query"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> actorIdOptions = new ArrayList<>();

        for (Object actor : actors.get(DATA)) {
            if (actor instanceof Map<?, ?> actordMap
                && actordMap.get("id") instanceof Map<?, ?> actorId
                && actordMap.get("values") instanceof Map<?, ?> actorValues
                && actorValues.get("primary_email_address") instanceof List<?> actorValueEmailList
                && actorValueEmailList.getFirst() instanceof Map<?, ?> actorValueEmail) {

                actorIdOptions
                    .add(option((String) actorValueEmail.get("email_address"), (String) actorId.get("record_id")));
            }
        }

        return actorIdOptions;
    }

    public static List<Option<String>> getTargetObjectOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, List<Object>> objects = context.http(http -> http.get("/objects"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> objectOptions = new ArrayList<>();

        for (Object object : objects.get(DATA)) {
            if (object instanceof Map<?, ?> objectMap) {
                objectOptions.add(
                    option((String) objectMap.get("singular_noun"), (String) objectMap.get("api_slug")));
            }
        }

        return objectOptions;
    }

    public static List<Option<String>> getRecordIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, inputParameters.getRequiredString(RECORD_TYPE));
    }

    public static List<Option<String>> getTargetRecordCompanyOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, COMPANIES);
    }

    public static List<Option<String>> getTargetRecordDealOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, DEALS);
    }

    private static List<Option<String>> getTargetRecordIdOptions(Context context, String targetObject) {

        Map<String, List<Object>> records = context.http(
            http -> http.post("/objects/%s/records/query".formatted(
                targetObject)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> recordIdOptions = new ArrayList<>();

        for (Object record : records.get(DATA)) {
            if (record instanceof Map<?, ?> recordMap
                && recordMap.get("id") instanceof Map<?, ?> recordId
                && recordMap.get("values") instanceof Map<?, ?> recordValues) {

                String recordName = "Unnamed person";

                if (recordValues.get("name") instanceof List<?> recordValueNameList &&
                    !recordValueNameList.isEmpty() &&
                    recordValueNameList.getFirst() instanceof Map<?, ?> recordValueName) {
                    if (targetObject.equals("people")) {
                        recordName = (String) recordValueName.get("full_name");
                    } else {
                        recordName = (String) recordValueName.get("value");
                    }
                }
                recordIdOptions.add(option(recordName, (String) recordId.get("record_id")));
            }
        }

        return recordIdOptions;
    }

    public static List<Option<String>> getTargetRecordIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, inputParameters.getRequiredString(TARGET_OBJECT));
    }

    public static List<Option<String>> getTargetRecordPersonOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, PEOPLE);
    }

    public static List<Option<String>> getTargetRecordWorkspaceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        return getTargetRecordIdOptions(context, WORKSPACES);
    }

    public static List<Option<String>> getWorkSpaceMemberIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        Map<String, List<Object>> actors = context.http(
            http -> http.get("/workspace_members"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> actorIdOptions = new ArrayList<>();

        for (Object actor : actors.get(DATA)) {
            if (actor instanceof Map<?, ?> actordMap
                && actordMap.get("id") instanceof Map<?, ?> actorId) {

                String fullName = actordMap.get("first_name") + " " + actordMap.get("last_name");

                actorIdOptions
                    .add(option(fullName, (String) actorId.get("workspace_member_id")));
            }
        }

        return actorIdOptions;
    }

    public static String subscribeWebhook(String eventType, Context context, String webhookUrl) {
        Map<String, Map<String, Object>> body = context.http(http -> http.post("/webhooks"))
            .body(
                Body.of(
                    DATA, Map.of(
                        "target_url", webhookUrl,
                        "subscriptions", List.of(
                            Map.of("event_type", eventType,
                                "filter", Map.of("$and", List.of()))))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String webhookId = "";

        if (body.get(DATA)
            .get(ID) instanceof Map<?, ?> idMap) {
            webhookId = idMap.get("webhook_id")
                .toString();
        }

        return webhookId;
    }

    public static void unsubscribeWebhook(Context context, String webhookId) {
        context.http(http -> http.delete("/webhooks/%s".formatted(webhookId)))
            .execute();
    }
}
