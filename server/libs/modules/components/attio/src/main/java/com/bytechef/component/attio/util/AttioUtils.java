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
import static com.bytechef.component.attio.constant.AttioConstants.COMPANY;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEALS;
import static com.bytechef.component.attio.constant.AttioConstants.FIRST_NAME;
import static com.bytechef.component.attio.constant.AttioConstants.FULL_NAME;
import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static com.bytechef.component.attio.constant.AttioConstants.LAST_NAME;
import static com.bytechef.component.attio.constant.AttioConstants.PEOPLE;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_ID;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.TARGET_OBJECT;
import static com.bytechef.component.attio.constant.AttioConstants.TARGET_RECORD_ID;
import static com.bytechef.component.attio.constant.AttioConstants.USERS;
import static com.bytechef.component.attio.constant.AttioConstants.VALUE;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACES;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * @author Nikolina Spehar
 */
public class AttioUtils {

    public static OptionsFunction<String> getCompanyIdOptions(String attribute) {
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
                    option((String) actorValueEmail.get("email_address"), (String) actorId.get(RECORD_ID)));
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

    public static OptionsFunction<String> getTargetRecordIdOptions(String targetObject) {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            String object;
            if (Objects.equals(targetObject, RECORD_TYPE)) {
                object = inputParameters.getRequiredString(RECORD_TYPE);
            } else if (Objects.equals(targetObject, TARGET_OBJECT)) {
                object = inputParameters.getRequiredFromPath(lookupDependsOnPaths.get(TARGET_OBJECT), String.class);
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
                        if (object.equals(PEOPLE)) {
                            recordName =
                                ((String) recordValueName.get(FULL_NAME)).isBlank() ? "Unnamed person"
                                    : (String) recordValueName.get(FULL_NAME);
                        } else {
                            recordName = (String) recordValueName.get(VALUE);
                        }
                    }

                    if (object.equals(USERS) &&
                        recordValues.get("primary_email_address") instanceof List<?> recordValueEmailList &&
                        recordValueEmailList.getFirst() instanceof Map<?, ?> recordValueEmail) {

                        recordName = (String) recordValueEmail.get("email_address");
                    }

                    recordIdOptions.add(option(recordName, (String) recordId.get(RECORD_ID)));
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
                String fullName = actor.get(FIRST_NAME) + " " + actor.get(LAST_NAME);

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

    private static String ifStringValueNull(Object value) {
        return value == null ? "" : value.toString();
    }

    public static Map<String, Object> getRecordValues(Map<String, Object> recordMap, String recordType) {
        Map<String, Object> values = new HashMap<>();

        for (Entry<String, Object> property : recordMap.entrySet()) {
            String key = property.getKey();

            if (recordMap.get(key) == null)
                break;
            switch (key) {
                case "value":
                    values.put(key, List.of(Map.of("currency_value", recordMap.get(key))));
                    break;
                case "domains":
                    values.put(key, List.of(Map.of("domain", recordMap.get(key))));
                    break;
                case "workspace_id", "avatar_url":
                    values.put(key, recordMap.get(key));
                    break;
                case "user_id", "stage", "owner":
                    if (recordType.equals(DEALS) && key.equals("owner")) {
                        values.put(key, List.of(Map.of("referenced_actor_id", recordMap.get(key),
                            "referenced_actor_type", "workspace-member")));
                    } else {
                        values.put(key, recordMap.get(key));
                    }
                    break;
                case "name", "description", "instagram", "facebook", "linkedin", "job_title":
                    if (recordType.equals(DEALS) || recordType.equals(WORKSPACES)) {
                        values.put(key, recordMap.get(key));
                    } else {
                        values.put(key, List.of(Map.of(VALUE, recordMap.get(key))));
                    }
                    break;
                case "foundation_date":
                    LocalDate foundationDate = LocalDate.parse(recordMap.get(key)
                        .toString());
                    values.put(key, List.of(Map.of(VALUE, foundationDate.toString())));
                    break;
                case "email_address":
                    if (recordType.equals(USERS)) {
                        values.put("primary_email_address", recordMap.get(key));
                    } else {
                        values.put("email_addresses", List.of(recordMap.get(key)));
                    }
                    break;

                case "estimated_arr_usd", "employee_range":
                    values.put(key, List.of(Map.of("option", recordMap.get(key))));
                    break;
                case "categories":
                    if (recordMap.get(key) instanceof List<?> categoriesList)
                        values.put(key, categoriesList.stream()
                            .map(category -> Map.of("option", category))
                            .toList());
                    break;
                case "associated_deals":
                    if (recordMap.get(key) instanceof List<?> dealsList)
                        values.put(key, dealsList.stream()
                            .map(deal -> Map.of(TARGET_OBJECT, DEALS, TARGET_RECORD_ID, deal))
                            .toList());
                    break;
                case "associated_users", "users":
                    if (recordMap.get(key) instanceof List<?> usersList)
                        values.put(key, usersList.stream()
                            .map(user -> Map.of(TARGET_OBJECT, USERS, TARGET_RECORD_ID, user))
                            .toList());
                    break;
                case "workspace", "associated_workspaces":
                    if (recordMap.get(key) instanceof List<?> workspacesList)
                        values.put(key, workspacesList.stream()
                            .map(workspace -> Map.of(TARGET_OBJECT, WORKSPACES, TARGET_RECORD_ID, workspace))
                            .toList());
                    break;
                case "associated_people":
                    if (recordMap.get(key) instanceof List<?> peopleList)
                        values.put("associated_people", peopleList.stream()
                            .map(person -> Map.of(TARGET_OBJECT, PEOPLE, TARGET_RECORD_ID, person))
                            .toList());
                    break;
                case "person":
                    values.put(key, Map.of(TARGET_OBJECT, PEOPLE, TARGET_RECORD_ID, recordMap.get(key)));
                    break;
                case COMPANY, "associated_company":
                    if (recordType.equals(WORKSPACES)) {
                        values.put(COMPANY, Map.of(TARGET_OBJECT, COMPANIES,
                            TARGET_RECORD_ID, recordMap.get(COMPANY)));
                    } else {
                        values.put(key, List.of(Map.of(TARGET_OBJECT, COMPANIES,
                            TARGET_RECORD_ID, recordMap.get(key))));
                    }
                    break;
                default:
                    break;
            }
        }

        if (recordType.equals(PEOPLE)) {
            addNameToValues(recordMap, values);
        }

        return values;
    }

    private static void addNameToValues(Map<String, Object> recordMap, Map<String, Object> values) {
        String firstName = ifStringValueNull(recordMap.get(FIRST_NAME));
        String lastName = ifStringValueNull(recordMap.get(LAST_NAME));
        String fullName = firstName + " " + lastName;

        values.put("name",
            List.of(Map.of(FIRST_NAME, firstName, LAST_NAME, lastName, FULL_NAME, fullName)));
    }

    public static ModifiableValueProperty<?, ?> getDealRecord(boolean isNewRecord) {
        return object(DEALS)
            .properties(
                string("name")
                    .label("Deal Name")
                    .description("The name of the deal.")
                    .required(isNewRecord),
                string("stage")
                    .label("Deal Stage")
                    .description("The stage of the deal.")
                    .options((OptionsFunction<String>) AttioUtils::getDealStageIdOptions)
                    .required(isNewRecord),
                string("owner")
                    .label("Deal Owner")
                    .description("The owner of the deal.")
                    .options((OptionsFunction<String>) AttioUtils::getWorkSpaceMemberIdOptions)
                    .required(isNewRecord),
                number(VALUE)
                    .label("Deal Value")
                    .description("The value of the deal.")
                    .required(false),
                array("associated_people")
                    .label("Associated People")
                    .description("The people associated with the deal.")
                    .options(getTargetRecordIdOptions(PEOPLE))
                    .required(false)
                    .items(
                        string(PEOPLE)
                            .label("People"))
                    .required(false),
                string("associated_company")
                    .label("Associated Company")
                    .description("The company associated with the deal.")
                    .options(getTargetRecordIdOptions(COMPANIES))
                    .required(false));
    }

    public static ModifiableValueProperty<?, ?> getUserRecord(boolean isNewRecord) {
        return object(USERS)
            .properties(
                string("person")
                    .label("Person")
                    .description("The person who will be the user.")
                    .options(getTargetRecordIdOptions(PEOPLE))
                    .required(false),
                string("email_address")
                    .label("Email Address")
                    .description("The email address of the user.")
                    .required(isNewRecord),
                string("user_id")
                    .label("User ID")
                    .description("The ID of the user.")
                    .required(isNewRecord),
                array("workspace")
                    .label("Associated Workspaces")
                    .description("The associated workspace of the company.")
                    .options(getTargetRecordIdOptions(WORKSPACES))
                    .required(false)
                    .items(
                        string("workspace")
                            .label("Workspace")
                            .required(false)));
    }

    public static ModifiableValueProperty<?, ?> getWorkspaceRecord(boolean isNewRecord) {
        return object(WORKSPACES)
            .properties(
                string("workspace_id")
                    .label("Workspace ID")
                    .description("The ID of the workspace.")
                    .required(isNewRecord),
                string("name")
                    .label("Name")
                    .description("The name of the workspace.")
                    .required(false),
                array(USERS)
                    .label("Users")
                    .description("The users in the workspace.")
                    .options((OptionsFunction<String>) AttioUtils::getTargetActorIdOptions)
                    .required(false)
                    .items(
                        string("user")
                            .label("Users")
                            .required(false)),
                string("company")
                    .label("Company")
                    .description("The company of the workspace.")
                    .options(getTargetRecordIdOptions(COMPANIES))
                    .required(false),
                string("avatar_url")
                    .label("Avatar URL")
                    .description("The URL of the avatar of the workspace.")
                    .required(false));
    }
}
