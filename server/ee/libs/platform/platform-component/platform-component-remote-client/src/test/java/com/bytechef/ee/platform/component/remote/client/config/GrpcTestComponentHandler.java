/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.config;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.propertyGroup;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.ComponentDsl.unifiedApi;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GrpcTestComponentHandler implements ComponentHandler {

    public static final String COMPONENT_NAME = "grpcTest";
    public static final String ECHO_ACTION = "echo";
    public static final String ITEM_HOOK_TRIGGER = "itemHook";
    public static final String LOOKUP_TOOL = "lookup";
    public static final String NEW_ITEM_TRIGGER = "newItem";
    public static final int POLL_LIMIT = 3;
    public static final String SLOW_ECHO_ACTION = "slowEcho";
    public static final Instant WEBHOOK_EXPIRATION_DATE = Instant.parse("2099-01-01T00:00:00Z");
    public static final Instant REFRESHED_WEBHOOK_EXPIRATION_DATE = Instant.parse("2099-02-01T00:00:00Z");

    private static final Queue<Map<String, Object>> DISABLED_WEBHOOKS = new ConcurrentLinkedQueue<>();

    private final ComponentDefinition componentDefinition;

    public GrpcTestComponentHandler(int version, String title) {
        this.componentDefinition = new GrpcTestComponentDefinition(
            component(COMPONENT_NAME)
                .title(title)
                .description("Exercises every remote component operation without leaving the process.")
                .version(version)
                .connection(
                    connection()
                        .baseUri((connectionParameters, context) -> "https://" +
                            connectionParameters.getString("region", "eu") + ".grpc-test.example.com")
                        .properties(string("region"))
                        .authorizations(
                            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                                .title("OAuth2")
                                .properties(string(Authorization.CLIENT_ID), string(Authorization.CLIENT_SECRET))
                                .authorizationUrl(
                                    (connectionParameters, context) -> "https://auth.grpc-test.example.com/authorize")
                                .scopes((connectionParameters, context) -> Map.of("items.read", true))
                                .refresh(
                                    (connectionParameters, context) -> new RefreshTokenResponse(
                                        "refreshed-" + connectionParameters.getString(Authorization.REFRESH_TOKEN),
                                        "next-refresh-token", 3600))))
                .inputs(
                    propertyGroup("inputs")
                        .properties(
                            string("status")
                                .options(
                                    (ActionDefinition.OptionsFunction<String>) GrpcTestComponentHandler::statusOptions)))
                .actions(
                    action(ECHO_ACTION)
                        .title("Echo")
                        .properties(
                            string("name")
                                .options(
                                    (ActionDefinition.OptionsFunction<String>) GrpcTestComponentHandler::nameOptions),
                            integer("count"),
                            dynamicProperties("fields")
                                .propertiesLookupDependsOn("name")
                                .properties((ActionDefinition.PropertiesFunction) GrpcTestComponentHandler::fields))
                        .output(
                            (ActionDefinition.OutputFunction) (
                                inputParameters, connectionParameters,
                                context) -> OutputResponse.of(
                                    object().properties(string("name"), integer("count")),
                                    Map.of("name", inputParameters.getString("name", ""), "count", 1)))
                        .workflowNodeDescription(
                            (ActionDefinition.WorkflowNodeDescriptionFunction) (
                                inputParameters,
                                context) -> "Echo " + inputParameters.getString("name"))
                        .perform((ActionDefinition.PerformFunction) GrpcTestComponentHandler::echo),
                    action(SLOW_ECHO_ACTION)
                        .title("Slow echo")
                        .properties(integer("millis"))
                        .perform(
                            (ActionDefinition.PerformFunction) (inputParameters, connectionParameters, context) -> {
                                Thread.sleep(inputParameters.getRequiredLong("millis"));

                                return "done";
                            }))
                .triggers(
                    trigger(NEW_ITEM_TRIGGER)
                        .title("New item")
                        .type(TriggerType.POLLING)
                        .properties(
                            string("status")
                                .options(
                                    (TriggerDefinition.OptionsFunction<String>) GrpcTestComponentHandler::statusOptions),
                            dynamicProperties("filters")
                                .propertiesLookupDependsOn("status")
                                .properties((TriggerDefinition.PropertiesFunction) GrpcTestComponentHandler::filters))
                        .output(
                            (TriggerDefinition.OutputFunction) (
                                inputParameters, connectionParameters,
                                context) -> OutputResponse.of(
                                    object().properties(string("id")), Map.of("id", "item-1")))
                        .workflowNodeDescription(
                            (TriggerDefinition.WorkflowNodeDescriptionFunction) (
                                inputParameters,
                                context) -> "New " + inputParameters.getString("status") + " item")
                        .poll(GrpcTestComponentHandler::poll),
                    trigger(ITEM_HOOK_TRIGGER)
                        .title("Item hook")
                        .type(TriggerType.DYNAMIC_WEBHOOK)
                        .properties(string("event"))
                        .webhookEnable(
                            (
                                inputParameters, connectionParameters, webhookUrl, workflowExecutionId,
                                context) -> new WebhookEnableOutput(
                                    Map.of(
                                        "event", inputParameters.getString("event"), "url", webhookUrl,
                                        "webhookId", "hook-" + workflowExecutionId),
                                    WEBHOOK_EXPIRATION_DATE))
                        .webhookDisable(
                            (
                                inputParameters, connectionParameters, outputParameters, workflowExecutionId,
                                context) -> DISABLED_WEBHOOKS.add(
                                    Map.of(
                                        "event", inputParameters.getString("event"), "webhookId",
                                        outputParameters.getString("webhookId"), "workflowExecutionId",
                                        workflowExecutionId)))
                        .dynamicWebhookRefresh(
                            (connectionParameters, outputParameters, context) -> new WebhookEnableOutput(
                                Map.of("refreshed", true, "webhookId", outputParameters.getString("webhookId")),
                                REFRESHED_WEBHOOK_EXPIRATION_DATE))
                        .webhookValidateOnEnable(
                            (
                                inputParameters, headers, parameters, body, method,
                                context) -> new WebhookValidateResponse(
                                    Map.of("challenge", parameters.firstValue("challenge")
                                        .orElse("")),
                                    Map.of("X-Event", List.of(inputParameters.getString("event"))), 200))
                        .webhookRequest(
                            (
                                inputParameters, connectionParameters, headers, parameters, body, method,
                                outputParameters,
                                context) -> Map.of("body", body.getContent(), "method", method.name())))
                .clusterElements(
                    clusterElement(LOOKUP_TOOL)
                        .title("Lookup")
                        .type(BaseToolFunction.TOOLS)
                        .properties(
                            string("query")
                                .options(
                                    (ClusterElementDefinition.OptionsFunction<String>) GrpcTestComponentHandler::queryOptions),
                            dynamicProperties("columns")
                                .propertiesLookupDependsOn("query")
                                .properties(
                                    (ClusterElementDefinition.PropertiesFunction) GrpcTestComponentHandler::columns))
                        .output(
                            (ClusterElementDefinition.OutputFunction) (
                                inputParameters, connectionParameters,
                                context) -> OutputResponse.of(
                                    object().properties(string("match")), Map.of("match", "row-1")))
                        .object(() -> (ToolFunction) GrpcTestComponentHandler::lookup)
                        .workflowNodeDescription(
                            (ClusterElementDefinition.WorkflowNodeDescriptionFunction) (
                                inputParameters,
                                context) -> "Look up " + inputParameters.getString("query")))
                .unifiedApi(unifiedApi(UnifiedApiCategory.CRM)));
    }

    public static void clearDisabledWebhooks() {
        DISABLED_WEBHOOKS.clear();
    }

    public static List<Map<String, Object>> getDisabledWebhooks() {
        return List.copyOf(DISABLED_WEBHOOKS);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Property.ValueProperty<?>> columns(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Object context) {

        return List.of(string(inputParameters.getString("query") + "Column"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object echo(Parameters inputParameters, Parameters connectionParameters, Object context) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("count", inputParameters.getInteger("count"));
        result.put("name", inputParameters.getString("name"));
        result.put("nested", Map.of("rows", List.of(Map.of("id", 1), Map.of("id", 2))));
        result.put("region", connectionParameters.getString("region"));

        return result;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Property.ValueProperty<?>> fields(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Object context) {

        List<Property.ValueProperty<?>> properties = new ArrayList<>();

        for (String fieldName : inputParameters.getList("fieldNames", String.class, List.of())) {
            properties.add(
                string(fieldName)
                    .label(fieldName + " of " + inputParameters.getString("name")));
        }

        return properties;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Property.ValueProperty<?>> filters(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        Object context) {

        return List.of(integer(inputParameters.getString("status") + "Since"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object lookup(Parameters inputParameters, Parameters connectionParameters, Object context) {
        return Map.of(
            "match", inputParameters.getString("query") + "-row", "region",
            connectionParameters.getString("region", "none"));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Option<String>> nameOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Object context) {

        String region = connectionParameters.getString("region", "none");

        return filterOptions(
            List.of(option("Ada in " + region, "ada"), option("Grace in " + region, "grace")), searchText);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters, Object context) {

        Integer cursor = closureParameters.getInteger("cursor");

        int position = cursor == null ? 0 : cursor;

        if (position >= POLL_LIMIT) {
            return new PollOutput(List.of(), Map.of("cursor", position), false);
        }

        return new PollOutput(
            List.of(Map.of("id", "item-" + (position + 1), "status", inputParameters.getString("status"))),
            Map.of("cursor", position + 1), false);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Option<String>> queryOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Object context) {

        return filterOptions(List.of(option("Customers", "customers"), option("Orders", "orders")), searchText);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static List<Option<String>> statusOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Object context) {

        return filterOptions(List.of(option("Open", "open"), option("Closed", "closed")), searchText);
    }

    private static List<Option<String>> filterOptions(List<Option<String>> options, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return options;
        }

        return options.stream()
            .filter(option -> option.getLabel()
                .toLowerCase()
                .contains(searchText.toLowerCase()))
            .toList();
    }

    private static class GrpcTestComponentDefinition extends AbstractComponentDefinitionWrapper
        implements ClusterRootComponentDefinition {

        GrpcTestComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(BaseToolFunction.TOOLS);
        }
    }
}
