package utils;

import com.bytechef.ai.mcp.tool.automation.ToolUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides high-level validation functionality for workflows, tasks, and task parameters.
 * This class orchestrates validation using specialized utility classes for different concerns.
 */
@Component
public class WorkflowValidator {

    private static final String INVALID_JSON_FORMAT = "Invalid JSON format: ";

    public static StringBuilder validateWorkflowStructure(String workflow, StringBuilder errors) {
        try {
            JsonNode workflowNode = WorkflowParser.parseJsonString(workflow);

            if (!workflowNode.isObject()) {
                errors.append("Workflow must be an object");
                return errors;
            }

            FieldValidator.validateRequiredStringField(workflowNode, "label", errors);
            FieldValidator.validateRequiredStringField(workflowNode, "description", errors);
            FieldValidator.validateWorkflowTriggers(workflowNode, errors);
            FieldValidator.validateRequiredArrayField(workflowNode, "tasks", errors);

        } catch (JsonProcessingException e) {
            handleJsonProcessingException(e, workflow, errors);
        }

        return errors;
    }

    public static StringBuilder validateTaskStructure(String task, StringBuilder errors) {
        try {
            JsonNode taskNode = WorkflowParser.parseJsonString(task);

            if (!taskNode.isObject()) {
                errors.append("Task must be an object");
                return errors;
            }

            FieldValidator.validateRequiredStringField(taskNode, "label", errors);
            FieldValidator.validateRequiredStringField(taskNode, "name", errors);
            FieldValidator.validateTaskType(taskNode, errors);
            FieldValidator.validateRequiredObjectField(taskNode, "parameters", errors);

        } catch (JsonProcessingException e) {
            errors.append(INVALID_JSON_FORMAT).append(e.getMessage()).append("\n");
        }

        return errors;
    }

    public static StringBuilder validateTaskParameters(String currentTaskParameters, String taskDefinition, StringBuilder errors, StringBuilder warnings) {
        try {
            JsonNode currentPropsNode = WorkflowParser.parseJsonString(currentTaskParameters);

            if (!currentPropsNode.isObject()) {
                errors.append("Current task parameters must be an object");
                return errors;
            }

            String processedTaskDefinition = WorkflowParser.processDisplayConditions(taskDefinition, currentTaskParameters);
            JsonNode taskDefNode = WorkflowParser.parseJsonString(processedTaskDefinition);

            if (!taskDefNode.isObject()) {
                errors.append("Task definition must be an object");
                return errors;
            }

            JsonNode parametersNode = taskDefNode.get("parameters");
            if (parametersNode == null || !parametersNode.isObject()) {
                errors.append("Task definition must have a 'parameters' object");
                return errors;
            }

            PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "", errors, warnings, taskDefinition, currentTaskParameters);

        } catch (RuntimeException e) {
            // Handle invalid display condition errors
            if (e.getMessage() != null && e.getMessage().startsWith("Invalid logic for display condition:")) {
                // Create a cleaned task definition by removing objects with invalid display conditions
                try {
                    String cleanedTaskDefinition = removeObjectsWithInvalidConditions(taskDefinition);
                    JsonNode taskDefNode = WorkflowParser.parseJsonString(cleanedTaskDefinition);
                    JsonNode parametersNode = taskDefNode.get("parameters");
                    if (parametersNode != null && parametersNode.isObject()) {
                        JsonNode currentPropsNode = WorkflowParser.parseJsonString(currentTaskParameters);
                        PropertyValidator.validatePropertiesRecursively(currentPropsNode, parametersNode, "", errors, warnings, cleanedTaskDefinition, currentTaskParameters);
                    }
                    // Add the invalid display condition error to warnings
                    ValidationErrorBuilder.append(warnings, e.getMessage());
                } catch (Exception ignored) {
                    // If we can't continue validation, that's fine, we've already reported the display condition error
                    ValidationErrorBuilder.append(warnings, e.getMessage());
                }
            } else {
                throw e; // Re-throw if it's not a display condition error
            }
        } catch (JsonProcessingException e) {
            errors.append(INVALID_JSON_FORMAT).append(e.getMessage()).append("\n");
        }

        return errors;
    }

    public static void validateWorkflowTasks(List<JsonNode> tasks, Map<String, String> taskDefinitions, Map<String, ToolUtils.PropertyInfo> taskOutput, StringBuilder errors, StringBuilder warnings) {
        List<String> taskNames = new ArrayList<>();
        Map<String, String> taskNameToTypeMap = new java.util.HashMap<>();

        for (JsonNode task : tasks) {
            String taskType = task.get("type").asText();
            String taskDefinition = taskDefinitions.get(taskType);

            validateTaskStructure(task.toString(), errors);

            String taskParameters = "{}";
            if (task.has("parameters") && task.get("parameters").isObject()) {
                taskParameters = task.get("parameters").toString();
            }
            validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

            String taskName = task.get("name").asText();
            taskNames.add(taskName);
            taskNameToTypeMap.put(taskName, taskType);

            boolean hasTaskOrderErrors = validateTaskDataPills(task, taskOutput, taskNames, taskNameToTypeMap, errors, warnings);

            // If task order errors occurred, stop processing further tasks
            if (hasTaskOrderErrors) {
                break;
            }
        }
    }

    public static boolean validateTaskDataPills(JsonNode task, Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames, Map<String, String> taskNameToTypeMap, StringBuilder errors, StringBuilder warnings) {
        if (!task.has("parameters") || !task.get("parameters").isObject()) {
            return false;
        }

        String currentTaskName = task.get("name").asText();

        int errorCountBefore = errors.length();

        // Use a flag to track if we should stop processing
        TaskValidationContext context = new TaskValidationContext();

        // Recursively find all data pills in the task parameters
        findDataPillsInNode(task.get("parameters"), "", currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context);

        // Return true if task order errors were found
        return errors.length() > errorCountBefore;
    }

    private static void findDataPillsInNode(JsonNode node, String currentPath, String currentTaskName,
                                           Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
                                           Map<String, String> taskNameToTypeMap, StringBuilder errors, StringBuilder warnings, TaskValidationContext context) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                if (context.stopProcessing) return;
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                findDataPillsInNode(fieldValue, newPath, currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                if (context.stopProcessing) break;
                findDataPillsInNode(node.get(i), currentPath + "[" + i + "]", currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context);
            }
        } else if (node.isTextual()) {
            if (!context.stopProcessing) {
                String textValue = node.asText();
                processDataPillsInText(textValue, currentPath, currentTaskName, taskOutput, taskNames, taskNameToTypeMap, errors, warnings, context);
            }
        }
    }

    private static void processDataPillsInText(String text, String fieldPath, String currentTaskName,
                                              Map<String, ToolUtils.PropertyInfo> taskOutput, List<String> taskNames,
                                              Map<String, String> taskNameToTypeMap, StringBuilder errors, StringBuilder warnings, TaskValidationContext context) {
        java.util.regex.Pattern dataPillPattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
        java.util.regex.Matcher matcher = dataPillPattern.matcher(text);

        while (matcher.find()) {
            if (context.stopProcessing) break;

            String dataPillExpression = matcher.group(1); // e.g., "testTask1.propString"

            if (dataPillExpression.contains(".")) {
                String[] parts = dataPillExpression.split("\\.", 2);
                String referencedTaskName = parts[0];
                String propertyName = parts[1];

                // Check if referenced task exists and is defined before current task
                int currentTaskIndex = taskNames.indexOf(currentTaskName);
                int referencedTaskIndex = taskNames.indexOf(referencedTaskName);

                if (referencedTaskIndex == -1 || referencedTaskIndex >= currentTaskIndex) {
                    ValidationErrorBuilder.append(errors, "Wrong task order: You can't reference '" + dataPillExpression + "' in " + currentTaskName + ".");
                    context.stopProcessing = true;
                    return;
                }

                // Get the task type for the referenced task from our mapping
                String referencedTaskType = taskNameToTypeMap.get(referencedTaskName);

                if (referencedTaskType != null) {
                    ToolUtils.PropertyInfo outputInfo = taskOutput.get(referencedTaskType);
                    if (outputInfo != null) {
                        // Check if the property exists in the task output
                        boolean propertyExists = checkPropertyExists(outputInfo, propertyName);

                        if (!propertyExists) {
                            ValidationErrorBuilder.append(warnings, "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType + "'");
                            continue;
                        }

                        // Check type compatibility based on the field where the data pill is used
                        String expectedType = getExpectedTypeFromFieldPath(fieldPath);
                        String actualType = getPropertyType(outputInfo, propertyName);

                        // Skip type validation if the field contains multiple data pills in a string context
                        // because all types can be converted to string when interpolated
                        if (expectedType != null && actualType != null && !isTypeCompatible(expectedType, actualType)) {
                            // If this is a string field containing multiple data pills, don't report type errors
                            if ("string".equalsIgnoreCase(expectedType) && isStringWithMultipleDataPills(text)) {
                                // Allow any type to be converted to string in interpolation
                                continue;
                            }
                            ValidationErrorBuilder.append(errors, "Property '" + dataPillExpression + "' in output of '" + referencedTaskType + "' is of type " + actualType.toLowerCase() + ", not " + expectedType.toLowerCase());
                        }
                    } else {
                        // Output info not found - add warning
                        ValidationErrorBuilder.append(warnings, "Property '" + dataPillExpression + "' might not exist in the output of '" + referencedTaskType + "'");
                    }
                }
            } else {
                // Data pill without property reference (e.g., ${invalidformat} or ${task1})
                if (!taskNames.contains(dataPillExpression)) {
                    ValidationErrorBuilder.append(errors, "Task '" + dataPillExpression + "' doesn't exits.");
                }
            }
        }
    }

    private static boolean checkPropertyExists(ToolUtils.PropertyInfo outputInfo, String propertyName) {
        return checkPropertyExistsRecursive(outputInfo, propertyName.split("\\."));
    }

    private static boolean checkPropertyExistsRecursive(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return true;
        }

        String currentProperty = propertyPath[0];

        // Handle array access like "items[0]"
        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

            // Check if the array property exists
            if (arrayName.equals(outputInfo.name()) ||
                (outputInfo.nestedProperties() != null &&
                 outputInfo.nestedProperties().stream().anyMatch(prop -> arrayName.equals(prop.name())))) {

                // Find the array property
                ToolUtils.PropertyInfo arrayProp = null;
                if (arrayName.equals(outputInfo.name())) {
                    arrayProp = outputInfo;
                } else if (outputInfo.nestedProperties() != null) {
                    arrayProp = outputInfo.nestedProperties().stream()
                        .filter(prop -> arrayName.equals(prop.name()))
                        .findFirst()
                        .orElse(null);
                }

                if (arrayProp != null && "ARRAY".equals(arrayProp.type()) && arrayProp.nestedProperties() != null && !arrayProp.nestedProperties().isEmpty()) {
                    // For arrays, use the first nested property (the array element type)
                    ToolUtils.PropertyInfo elementType = arrayProp.nestedProperties().get(0);
                    if (propertyPath.length == 1) {
                        return true; // Just accessing the array element
                    }
                    // Continue with the remaining path
                    String[] remainingPath = new String[propertyPath.length - 1];
                    System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                    return checkPropertyExistsRecursive(elementType, remainingPath);
                }
            }
            return false;
        }

        // Check if the current property matches the main property
        if (currentProperty.equals(outputInfo.name())) {
            if (propertyPath.length == 1) {
                return true; // This is the final property in the path
            }
            // Continue with nested properties of this object
            if (outputInfo.nestedProperties() != null) {
                String[] remainingPath = new String[propertyPath.length - 1];
                System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                    if (checkPropertyExistsRecursive(nestedProp, remainingPath)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // Check nested properties if they exist
        if (outputInfo.nestedProperties() != null) {
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (currentProperty.equals(nestedProp.name())) {
                    if (propertyPath.length == 1) {
                        return true; // This is the final property in the path
                    }
                    // Continue with nested properties
                    if (nestedProp.nestedProperties() != null) {
                        String[] remainingPath = new String[propertyPath.length - 1];
                        System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                        for (ToolUtils.PropertyInfo deepNestedProp : nestedProp.nestedProperties()) {
                            if (checkPropertyExistsRecursive(deepNestedProp, remainingPath)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }
        }

        return false;
    }

    private static String getPropertyType(ToolUtils.PropertyInfo outputInfo, String propertyName) {
        return getPropertyTypeRecursive(outputInfo, propertyName.split("\\."));
    }

    private static String getPropertyTypeRecursive(ToolUtils.PropertyInfo outputInfo, String[] propertyPath) {
        if (propertyPath.length == 0) {
            return outputInfo.type();
        }

        String currentProperty = propertyPath[0];

        // Handle array access like "items[0]"
        if (currentProperty.contains("[") && currentProperty.endsWith("]")) {
            String arrayName = currentProperty.substring(0, currentProperty.indexOf('['));

            // Check if the array property exists
            if (arrayName.equals(outputInfo.name()) ||
                (outputInfo.nestedProperties() != null &&
                 outputInfo.nestedProperties().stream().anyMatch(prop -> arrayName.equals(prop.name())))) {

                // Find the array property
                ToolUtils.PropertyInfo arrayProp = null;
                if (arrayName.equals(outputInfo.name())) {
                    arrayProp = outputInfo;
                } else if (outputInfo.nestedProperties() != null) {
                    arrayProp = outputInfo.nestedProperties().stream()
                        .filter(prop -> arrayName.equals(prop.name()))
                        .findFirst()
                        .orElse(null);
                }

                if (arrayProp != null && "ARRAY".equals(arrayProp.type()) && arrayProp.nestedProperties() != null && !arrayProp.nestedProperties().isEmpty()) {
                    // For arrays, use the first nested property (the array element type)
                    ToolUtils.PropertyInfo elementType = arrayProp.nestedProperties().get(0);
                    if (propertyPath.length == 1) {
                        return elementType.type(); // Return the type of the array element
                    }
                    // Continue with the remaining path
                    String[] remainingPath = new String[propertyPath.length - 1];
                    System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                    return getPropertyTypeRecursive(elementType, remainingPath);
                }
            }
            return null;
        }

        // Check if the current property matches the main property
        if (currentProperty.equals(outputInfo.name())) {
            if (propertyPath.length == 1) {
                return outputInfo.type(); // This is the final property in the path
            }
            // Continue with nested properties of this object
            if (outputInfo.nestedProperties() != null) {
                String[] remainingPath = new String[propertyPath.length - 1];
                System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                    String result = getPropertyTypeRecursive(nestedProp, remainingPath);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }

        // Check nested properties if they exist
        if (outputInfo.nestedProperties() != null) {
            for (ToolUtils.PropertyInfo nestedProp : outputInfo.nestedProperties()) {
                if (currentProperty.equals(nestedProp.name())) {
                    if (propertyPath.length == 1) {
                        return nestedProp.type(); // This is the final property in the path
                    }
                    // Continue with nested properties
                    if (nestedProp.nestedProperties() != null) {
                        String[] remainingPath = new String[propertyPath.length - 1];
                        System.arraycopy(propertyPath, 1, remainingPath, 0, propertyPath.length - 1);
                        for (ToolUtils.PropertyInfo deepNestedProp : nestedProp.nestedProperties()) {
                            String result = getPropertyTypeRecursive(deepNestedProp, remainingPath);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                    return null;
                }
            }
        }

        return null;
    }

    private static String getExpectedTypeFromFieldPath(String fieldPath) {
        // Extract the field name from the path
        String fieldName = fieldPath;
        if (fieldPath.contains(".")) {
            String[] parts = fieldPath.split("\\.");
            fieldName = parts[parts.length - 1];
        }

        // Determine expected type based on common field names
        switch (fieldName) {
            case "active":
                return "boolean";
            case "name":
                return "string";
            default:
                return null;
        }
    }

    private static boolean isTypeCompatible(String expectedType, String actualType) {
        if (expectedType == null || actualType == null) {
            return true;
        }
        return expectedType.equalsIgnoreCase(actualType);
    }

    private static boolean isStringWithMultipleDataPills(String text) {
        java.util.regex.Pattern dataPillPattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)}");
        java.util.regex.Matcher matcher = dataPillPattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    private static class TaskValidationContext {
        boolean stopProcessing = false;
    }

    private static String removeObjectsWithInvalidConditions(String taskDefinition) {
        try {
            String result = taskDefinition;

            // Remove objects that have metadata with @ symbols (display conditions)
            result = result.replaceAll("\"[^\"]+\"\\s*:\\s*\\{[^{}]*\"metadata\"\\s*:\\s*\"[^\"]*@[^@]*@[^\"]*\"[^{}]*\\}", "");

            // Clean up any resulting JSON syntax issues
            result = WorkflowParser.cleanupJsonSyntax(result);

            return result;
        } catch (Exception e) {
            return taskDefinition; // Return original if cleaning fails
        }
    }


    public static void handleJsonProcessingException(JsonProcessingException e, String workflow, StringBuilder errors) {
        if (e.getMessage() != null && workflow.contains("\"type\":") && workflow.contains("triggers")) {
            errors.append("Trigger must be an object\n");
        } else {
            errors.append(INVALID_JSON_FORMAT).append(e.getMessage()).append("\n");
        }
    }
}
