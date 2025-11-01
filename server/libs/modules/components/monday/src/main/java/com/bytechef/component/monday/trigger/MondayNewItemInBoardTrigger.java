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

package com.bytechef.component.monday.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.monday.constant.MondayColumnType.getColumnTypeByName;
import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.END_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.FROM;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.START_DATE;
import static com.bytechef.component.monday.constant.MondayConstants.TEXT;
import static com.bytechef.component.monday.constant.MondayConstants.TO;
import static com.bytechef.component.monday.constant.MondayConstants.TYPE;
import static com.bytechef.component.monday.constant.MondayConstants.VALUE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.monday.constant.MondayColumnType;
import com.bytechef.component.monday.util.MondayOptionUtils;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayNewItemInBoardTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newItemInBoard")
        .title("New Item in Board")
        .description("Triggers when an item is created in board.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .webhookValidateOnEnable(MondayNewItemInBoardTrigger::webhookValidateOnEnable)
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(true),
            string(BOARD_ID)
                .label("Board ID")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true))
        .output()
        .webhookEnable(MondayNewItemInBoardTrigger::webhookEnable)
        .webhookDisable(MondayNewItemInBoardTrigger::webhookDisable)
        .webhookRequest(MondayNewItemInBoardTrigger::webhookRequest);

    private MondayNewItemInBoardTrigger() {
    }

    public static WebhookValidateResponse webhookValidateOnEnable(
        Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
        WebhookMethod method, TriggerContext context) {

        return new WebhookValidateResponse(body.getContent(), 200);
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        String query = "mutation{create_webhook(board_id: %s, url: \"%s\", event: create_item){id}}"
            .formatted(inputParameters.getRequiredString(BOARD_ID), webhookUrl);

        Map<String, Object> body = executeGraphQLQuery(query, context);

        if (body.get(DATA) instanceof Map<?, ?> map && map.get("create_webhook") instanceof Map<?, ?> webhookMap) {
            return new WebhookEnableOutput(Map.of(ID, webhookMap.get(ID)), null);
        }

        throw new ProviderException("Failed to start Monday webhook.");
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        String query = "mutation{delete_webhook(id: " + outputParameters.getString(ID) + "){id}}";

        executeGraphQLQuery(query, context);
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        Map<String, Map<String, Object>> content = body.getContent(new TypeReference<>() {});

        Map<String, Object> event = content.get("event");

        String query = "query{boards(ids:" + event.get("boardId") + ")" +
            "{items_page(query_params:{ids: " + event.get("pulseId") + "})" +
            "{items{id name column_values {column{id title} id type value text " +
            "... on WeekValue{start_date end_date}}}}}}";

        event.put("columnValues", transformColumnValues(context, query));

        return event;
    }

    private static Map<String, Object> transformColumnValues(TriggerContext context, String query) {
        Map<String, Object> result = executeGraphQLQuery(query, context);

        Map<String, Object> transformedValues = new HashMap<>();

        if (result.get(DATA) instanceof Map<?, ?> map &&
            map.get(BOARDS) instanceof List<?> list &&
            list.getFirst() instanceof Map<?, ?> boardMap &&
            boardMap.get("items_page") instanceof Map<?, ?> itemsPageMap &&
            itemsPageMap.get("items") instanceof List<?> itemList &&
            itemList.getFirst() instanceof Map<?, ?> itemMap &&
            itemMap.get("column_values") instanceof List<?> columnValues) {

            for (Object o : columnValues) {
                if (o instanceof Map<?, ?> columnValueMap) {
                    String type = (String) columnValueMap.get(TYPE);

                    if (columnValueMap.get("column") instanceof Map<?, ?> columnMap) {
                        MondayColumnType enumType = getColumnTypeByName(type);

                        String id = (String) columnMap.get(ID);

                        switch (enumType) {
                            case CHECKBOX -> {
                                Map<String, ?> value1 =
                                    context.json(json -> json.readMap((String) columnValueMap.get(VALUE)));

                                transformedValues.put(id, value1.get("checked"));
                            }
                            case COUNTRY, DROPDOWN, EMAIL, HOUR, LINK, LONG_TEXT, TEXT, STATUS, WORLD_CLOCK ->
                                transformedValues.put(id, columnValueMap.get(TEXT));
                            case DATE -> {
                                String text = (String) columnValueMap.get(TEXT);

                                if (text != null && !text.isEmpty()) {
                                    transformedValues.put(id, LocalDate.parse(text));
                                }
                            }
                            case LOCATION -> {
                                Map<String, ?> value1 =
                                    context.json(json -> json.readMap((String) columnValueMap.get(VALUE)));
                                transformedValues.put(id, value1);
                            }
                            case RATING ->
                                transformedValues.put(id, Integer.valueOf((String) columnValueMap.get(TEXT)));
                            case NUMBERS ->
                                transformedValues.put(id, Double.valueOf((String) columnValueMap.get(TEXT)));
                            case TIMELINE -> {
                                Map<String, ?> value1 =
                                    context.json(json -> json.readMap((String) columnValueMap.get(VALUE)));

                                transformedValues.put(id,
                                    Map.of(
                                        "Start Date", LocalDate.parse((String) value1.get(FROM)),
                                        "End Date", LocalDate.parse((String) value1.get(TO))));
                            }
                            case WEEK -> {
                                Map<String, ?> value1 =
                                    context.json(json -> json.readMap((String) columnValueMap.get(VALUE)));

                                if (value1.get("week") instanceof Map<?, ?> weekMap) {
                                    String startDate = (String) weekMap.get(START_DATE);
                                    String endDate = (String) weekMap.get(END_DATE);

                                    transformedValues.put(id,
                                        Map.of(
                                            "Start Date", LocalDate.parse(startDate),
                                            "End Date", LocalDate.parse(endDate)));
                                }
                            }
                            default -> {
                            }
                        }
                    }
                }
            }
        }
        return transformedValues;
    }

}
