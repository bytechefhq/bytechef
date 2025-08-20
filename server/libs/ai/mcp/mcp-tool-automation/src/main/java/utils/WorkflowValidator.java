package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

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

    private static String removeObjectsWithInvalidConditions(String taskDefinition) {
        try {
            JsonNode taskDefNode = WorkflowParser.parseJsonString(taskDefinition);
            // For now, simply remove all objects with metadata fields that contain @ symbols
            // This is a simple approach - a more sophisticated approach would parse and validate each condition
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
