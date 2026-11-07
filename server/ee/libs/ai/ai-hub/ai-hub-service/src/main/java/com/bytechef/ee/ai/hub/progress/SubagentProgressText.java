/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import com.bytechef.commons.util.JsonUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentProgressText {

    static final int MAX_LENGTH = 80;

    private static final int MAX_NAME_LENGTH = 40;

    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])");
    private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
        "(?i)\\b(api[ _-]?key|access[ _-]?key|secret|token|password|passwd|bearer|authorization)\\b(\\s*[:=]\\s*)\\S+");
    private static final Pattern LONG_TOKEN = Pattern.compile("[A-Za-z0-9_\\-+/=.]{24,}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final List<String> REQUEST_FIELD_NAMES =
        List.of("request", "description", "prompt", "query", "task");

    private SubagentProgressText() {
    }

    public static String describeRequest(@Nullable String toolInput) {
        Map<String, ?> input = parse(toolInput);

        for (String fieldName : REQUEST_FIELD_NAMES) {
            if (input.get(fieldName) instanceof String request && !request.isBlank()) {
                return truncate(maskCredentials(firstSentence(request)), MAX_LENGTH);
            }
        }

        return "";
    }

    public static String describeStep(String toolName, @Nullable String toolInput) {
        Map<String, ?> input = parse(toolInput);

        String text = switch (toolName) {
            case "getClusterElementsInstructions", "getScriptCodeInstructions", "getTaskDispatcherBuildInstructions",
                "getWorkflowBuildInstructions" -> "Reading the build instructions";
            case "listActions", "listComponents", "listTasks", "listTriggers", "searchActions",
                "searchClusterElements", "searchComponents", "searchTasks", "searchTriggers" -> "Searching components";
            case "getTask", "getTaskDefinition", "getTaskOutputProperty", "getTaskProperties" -> describeTask(input);
            case "getTrigger", "getTriggerDefinition" -> addingTrigger(input, "triggerName");
            case "getAction", "getActionDefinition" -> configuring(input, "actionName");
            case "getClusterElement" -> configuring(input, "clusterElementName");
            case "getOutputProperty", "getProperties" -> configuring(input, "operationName");
            case "getComponent" -> "Looking at " + quote(fetchName(input, "componentName"));
            case "lookupPropertyOptions" -> "Looking up options for " + quote(
                operation(fetchName(input, "componentName"), fetchName(input, "operationName")));
            case "validateTask" -> "Checking " + quote(
                operation(fetchName(input, "componentName"), fetchName(input, "name")));
            case "validateWorkflow" -> "Validating the workflow";
            case "getWorkflow" -> "Reading the workflow";
            case "listWorkflows", "searchWorkflows" -> "Looking up workflows";
            case "createProjectWorkflow" -> "Creating the workflow";
            case "updateWorkflow" -> "Saving the workflow";
            case "deleteWorkflow" -> "Removing a workflow";
            case "saveWorkflowTestConnection" -> describeConnection(input);
            case "updateScriptComponentCode" -> "Writing the script code";
            case "listDataTables" -> "Looking up data tables";
            case "createDataTable" -> "Creating a data table";
            case "askUserQuestion" -> "Asking you a question";
            default -> humanize(toolName);
        };

        return truncate(text, MAX_LENGTH);
    }

    private static String describeTask(Map<String, ?> input) {
        String type = fetchName(input, "type");

        if ("trigger".equals(type)) {
            return addingTrigger(input, "name");
        }

        if ("taskDispatcher".equals(type)) {
            String name = fetchName(input, "name");

            return name.isEmpty() ? "Adding a flow step" : "Adding a " + quote(name) + " step";
        }

        return configuring(input, "name");
    }

    private static String addingTrigger(Map<String, ?> input, String nameKey) {
        String operation = operation(fetchName(input, "componentName"), fetchName(input, nameKey));

        return operation.isEmpty() ? "Adding the trigger" : "Adding the trigger " + quote(operation);
    }

    private static String configuring(Map<String, ?> input, String nameKey) {
        String operation = operation(fetchName(input, "componentName"), fetchName(input, nameKey));

        return operation.isEmpty() ? "Configuring a step" : "Configuring " + quote(operation);
    }

    private static String describeConnection(Map<String, ?> input) {
        String connectionKey = fetchName(input, "connectionKey");

        if (connectionKey.isEmpty()) {
            connectionKey = fetchName(input, "workflowNodeName");
        }

        return connectionKey.isEmpty() ? "Connecting" : "Connecting " + quote(connectionKey);
    }

    private static String operation(String componentName, String operationName) {
        if (componentName.isEmpty()) {
            return operationName;
        }

        if (operationName.isEmpty()) {
            return componentName;
        }

        return componentName + " › " + operationName;
    }

    private static String quote(String value) {
        return value.isEmpty() ? "" : "\"" + value + "\"";
    }

    private static String fetchName(Map<String, ?> input, String key) {
        if (!(input.get(key) instanceof String value)) {
            return "";
        }

        return truncate(maskCredentials(collapseWhitespace(value)), MAX_NAME_LENGTH);
    }

    private static String humanize(String toolName) {
        String spaced = CAMEL_CASE_BOUNDARY.matcher(toolName)
            .replaceAll(" ");

        String lowerCased = spaced.toLowerCase(Locale.ROOT)
            .replace('_', ' ');

        if (lowerCased.isBlank()) {
            return "Working";
        }

        return Character.toUpperCase(lowerCased.charAt(0)) + lowerCased.substring(1);
    }

    private static String firstSentence(String request) {
        String text = request;

        int cutIndex = text.length();

        for (char stopCharacter : new char[] {
            '\n', '{', '['
        }) {
            int index = text.indexOf(stopCharacter);

            if (index >= 0 && index < cutIndex) {
                cutIndex = index;
            }
        }

        text = text.substring(0, cutIndex);

        String collapsed = collapseWhitespace(text);

        if (collapsed.endsWith(":")) {
            collapsed = collapsed.substring(0, collapsed.length() - 1)
                .strip();
        }

        return collapsed;
    }

    private static String maskCredentials(String text) {
        String masked = CREDENTIAL_ASSIGNMENT.matcher(text)
            .replaceAll("$1$2•••");

        return LONG_TOKEN.matcher(masked)
            .replaceAll("•••");
    }

    private static String collapseWhitespace(String text) {
        return WHITESPACE.matcher(text)
            .replaceAll(" ")
            .strip();
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 1)
            .stripTrailing() + "…";
    }

    private static Map<String, ?> parse(@Nullable String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }

        try {
            return JsonUtils.readMap(toolInput);
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }
}
