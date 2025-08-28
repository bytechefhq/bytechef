package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.bytechef.ai.mcp.tool.automation.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowValidatorTest {
    private static final ToolUtils.PropertyInfo trigger1 = new ToolUtils.PropertyInfo("propString", "STRING", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action1 = new ToolUtils.PropertyInfo("propBool", "BOOLEAN", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action2 = new ToolUtils.PropertyInfo("propNumber", "NUMBER", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action3 = new ToolUtils.PropertyInfo("propInteger", "INTEGER", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo actionObj = new ToolUtils.PropertyInfo(
        "item", "OBJECT", null, false, true, null, List.of(
            action1, action2, action3
    ));
    private static final ToolUtils.PropertyInfo actionArr = new ToolUtils.PropertyInfo(
        "items", "ARRAY", null, false, true, null, List.of(
            //unnamed objects don't have to be referenced
        new ToolUtils.PropertyInfo(null, "OBJECT", null, false, true, null, List.of(
        action1, action2, action3
    ))));

    @Test
    void validateWorkflowStructure_validWorkflow_noErrors() {
        String validWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(validWorkflow, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingLabel_addsError() {
        String invalidWorkflow = """
        {
            "description": "workflowDescription",
            "inputs": [],
            "triggers": [
                {
                    "label": "Manual",
                    "name": "trigger_1",
                    "type": "manual/v1/manual"
                }
            ],
            "tasks": []
        }
        """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: label", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonStringLabel_addsError() {
        String invalidWorkflow = """
            {
                "label": 123,
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'label' must be a string", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingDescription_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: description", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonStringDescription_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": true,
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'description' must be a string", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: triggers", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonArrayTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": "not an array",
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'triggers' must be an array", errors.toString());
    }

    @Test
    void validateWorkflowStructure_multipleTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    },
                    {
                        "label": "Manual",
                        "name": "trigger_2",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'triggers' must contain exactly one object", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonObjectTrigger_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                   "manual/v1/manual"
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Trigger must be an object", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingTasks_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: tasks", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonArrayTasks_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": "not an array"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'tasks' must be an array", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonObject_addsError() {
        String invalidWorkflow = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertTrue(errors.toString().contains("Workflow must be an object"));
    }

    @Test
    void validateWorkflowStructure_invalidJson_addsError() {
        String invalidWorkflow = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertTrue(errors.toString().contains("Invalid JSON format:"), errors.toString());
    }

    @Test
    void validateTaskStructure_validTask_noErrors() {
        String validTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(validTask, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskStructure_missingLabel_addsError() {
        String invalidTask = """
            {
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: label", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringLabel_addsError() {
        String invalidTask = """
            {
                "label": 123,
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'label' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_missingName_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: name", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringName_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": 123,
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'name' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_missingType_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: type", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringType_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": 123,
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'type' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_invalidTypePattern_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertTrue(errors.toString().contains("Field 'type' must match pattern:"), errors.toString());
    }

    @Test
    void validateTaskStructure_validTypePatterns_noErrors() {
        String[] validTypes = {
            "component/v1",
            "component/v1/action",
            "comp123/v456/act789"
        };

        for (String type : validTypes) {
            String validTask = String.format("""
                {
                    "label": "Test Task",
                    "name": "testTask",
                    "type": "%s",
                    "parameters": {}
                }
                """, type);

            StringBuilder errors = new StringBuilder();
            WorkflowValidator.validateTaskStructure(validTask, errors);

            assertEquals("", errors.toString(), "Type should be valid: " + type);
        }
    }

    @Test
    void validateTaskStructure_missingParameters_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: parameters", errors.toString());
    }

    @Test
    void validateTaskStructure_nonObjectParameters_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": "not an object"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'parameters' must be an object", errors.toString());
    }

    @Test
    void validateTaskStructure_nonObject_addsError() {
        String invalidTask = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Task must be an object", errors.toString());
    }

    @Test
    void validateTaskStructure_invalidJson_addsError() {
        String invalidTask = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertTrue(errors.toString().contains("Invalid JSON format:"), errors.toString());
    }

    @Test
    void validateTaskParameters_validParameters_noErrors() {
        String currentTaskParameters = """
            {
                "name": "John",
                "age": 30,
                "active": true
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "age": "number",
                    "active": "boolean"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_extraProperty_addsWarning() {
        String currentTaskParameters = """
            {
                "name": "John",
                "extraField": "not allowed"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'extraField' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_missingRequiredProperty_addsError() {
        String currentTaskParameters = """
            {
                "age": 30
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "age": "number"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_wrongType_addsError() {
        String currentTaskParameters = """
            {
                "name": 123,
                "age": "thirty",
                "active": "yes"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "age": "integer",
                    "active": "boolean"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'name' has incorrect type. Expected: string, but got: integer
            Property 'age' has incorrect type. Expected: integer, but got: string
            Property 'active' has incorrect type. Expected: boolean, but got: string""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_wrongTypeInArrayAndObject_addsError() {
        String currentTaskParameters = """
            {
                "obj": {
                    "name": 123,
                    "age": "thirty",
                    "active": "yes",
                    "items": ["John", 36, "Porky"]
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "obj": {
                        "name": "string (required)",
                        "age": "integer",
                        "active": "boolean",
                        "items": ["integer"]
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'obj.name' has incorrect type. Expected: string, but got: integer
            Property 'obj.age' has incorrect type. Expected: integer, but got: string
            Property 'obj.active' has incorrect type. Expected: boolean, but got: string
            Value 'John' has incorrect type in property 'items'. Expected: integer, but got: string
            Value 'Porky' has incorrect type in property 'items'. Expected: integer, but got: string""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_arrayAndObjectTypes_noErrors() {
        String currentTaskParameters = """
            {
                "items": [],
                "config": {}
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "items": [],
                    "config": {}
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_typesInArrayAndObject_noErrors() {
        String currentTaskParameters = """
            {
                "items": ["string"],
                "config": {
                    "key": "string (required)"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "items": "array (required)",
                    "config": "object"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'config.key' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_nonObjectCurrentParameters_addsError() {
        String currentTaskParameters = "\"not an object\"";

        String taskDefinition = """
            {
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Current task parameters must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nonObjectTaskDefinition_addsError() {
        String currentTaskParameters = """
            {
                "name": "John"
            }
            """;

        String taskDefinition = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Task definition must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_missingParametersInDefinition_addsError() {
        String currentTaskParameters = """
            {
                "name": "John"
            }
            """;

        String taskDefinition = """
            {
                "otherField": "value"
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Task definition must have a 'parameters' object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_invalidJsonCurrentParameters_addsError() {
        String currentTaskParameters = "{invalid json}";

        String taskDefinition = """
            {
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertTrue(errors.toString().contains("Invalid JSON format:"), errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_invalidJsonTaskDefinition_addsError() {
        String currentTaskParameters = """
            {
                "name": "John"
            }
            """;

        String taskDefinition = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertTrue(errors.toString().contains("Invalid JSON format:"), errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nullValue_correctType() {
        String currentTaskParameters = """
            {
                "nullable": null
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "nullable": "string"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Property 'nullable' has incorrect type. Expected: string, but got: null", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_mixedValidationScenario() {
        String currentTaskParameters = """
            {
                "validString": "hello",
                "invalidNumber": "not an integer",
                "extraProperty": "can be here"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "validString": "string (required)",
                    "invalidNumber": "integer (required)",
                    "missingRequired": "boolean (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'invalidNumber' has incorrect type. Expected: integer, but got: string
            Missing required property: missingRequired""", errors.toString());
        assertEquals("Property 'extraProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_emptyParametersDefinition_allowsAnyParameters() {
        String currentTaskParameters = """
            {
                "anyProperty": "any value"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'anyProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_caseInsensitiveTypeMatching() {
        String currentTaskParameters = """
            {
                "upperCaseType": "test"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "upperCaseType": "STRING (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());

    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalProperty() {
        String currentTaskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "boolean (required)",
                    "featureConfig": { "metadata": "@enableFeature == true@", "setting1": "string (required)", "setting2": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyReverseOrder() {
        String currentTaskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "boolean (required)",
                    "featureConfig": { "metadata": "@true == enableFeature@", "setting1": "string (required)", "setting2": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyString() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "string (required)",
                    "featureConfig": { "metadata": "@enableFeature == 'true'@", "setting1": "string (required)", "setting2": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyContains() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "string (required)",
                    "featureConfig": {
                        "metadata": "@contains({'true','True','1'}, enableFeature)@",
                        "setting1": "string (required)",
                        "setting2": "string"
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionFalse_includesConditionalPropertyContains() {
        String currentTaskParameters = """
            {
                "enableFeature": "false",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "string (required)",
                    "featureConfig": {
                        "metadata": "@contains({'true','True','1'}, enableFeature)@",
                        "setting1": "string (required)",
                        "setting2": "string"
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_invalidDisplayConditionOperation() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "string (required)",
                    "featureConfig": {
                        "metadata": "@gndfknskgn / sflakdjdkf 3@",
                        "setting1": "string (required)",
                        "setting2": "string"
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition
            Invalid logic for display condition: '@gndfknskgn / sflakdjdkf 3@'""", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionFalseFloat_errorWhenConditionalPropertyProvided() {
        String currentTaskParameters = """
            {
                "enableFeature": 4,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "integer (required)",
                    "featureConfig": { "metadata": "@enableFeature <= 5.87546@", "setting1": "string (required)", "setting2": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrueIntegerInverted_errorWhenConditionalPropertyProvided() {
        String currentTaskParameters = """
            {
                "enableFeature": 4.1,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "enableFeature": "float (required)",
                    "featureConfig": { "metadata": "@50 > enableFeature", "setting1": "string (required)", "setting2": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_multipleDisplayConditions_filtersByCondition_incorrectCondition() {
        String currentTaskParameters = """
            {
                "mode": "advanced",
                "basicConfig": {
                    "name": "basic"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "mode": "string (required)",
                    "basicConfig": { "metadata": "@mode == 'basic'@", "name": "string (required)" },
                    "advancedConfig": { "metadata": "@mode == 'advanced'@", "name": "string (required)", "extra": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig.name", errors.toString());
        assertEquals("""
            Property 'basicConfig' is not defined in task definition
            Property 'basicConfig.name' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_includesCorrectOne() {
        String currentTaskParameters = """
            {
                "bodyContentType": true,
                "bodyContent": {
                    "extension": "txt",
                    "mimeType": "text/plain",
                    "name": "file.txt",
                    "url": "https://example.com/file.txt"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "bodyContentType": "boolean",
                    "bodyContent": { "metadata": "@bodyContentType == true@", "extension": "string (required)", "mimeType": "string (required)", "name": "string (required)", "url": "string (required)" },
                    "bodyContent": { "metadata": "@bodyContentType == false@" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_excludesIncorrectOne() {
        String currentTaskParameters = """
            {
                "bodyContentType": false,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "bodyContentType": "boolean",
                    "bodyContent": { "metadata": "@bodyContentType == true@", "extension": "string (required)", "mimeType": "string (required)", "name": "string (required)", "url": "string (required)" },
                    "bodyContent": { "metadata": "@bodyContentType == false@", "simpleProperty": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'bodyContent.extension' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_includesIncorrect() {
        String currentTaskParameters = """
            {
                "bodyContentType": true,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "bodyContentType": "boolean",
                    "bodyContent": { "metadata": "@bodyContentType == true@", "extension": "string (required)", "mimeType": "string (required)", "name": "string (required)", "url": "string (required)" },
                    "bodyContent": { "metadata": "@bodyContentType == false@", "simpleProperty": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: bodyContent.mimeType
            Missing required property: bodyContent.name
            Missing required property: bodyContent.url""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedDisplayConditions_validatesCorrectly() {
        String currentTaskParameters = """
            {
                "parentEnabled": true,
                "parent": {
                    "childEnabled": true,
                    "child": {
                        "value": "test"
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "parentEnabled": "boolean (required)",
                    "parent": {
                        "metadata": "@parentEnabled == true@",
                        "childEnabled": "boolean (required)",
                        "child": { "metadata": "@childEnabled == true@", "value": "string (required)" }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_stringValueCondition_worksCorrectly() {
        String currentTaskParameters = """
            {
                "format": "json",
                "jsonConfig": {
                    "indent": 2
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "format": "string (required)",
                    "jsonConfig": { "metadata": "@format == 'json'@", "indent": "integer" },
                    "xmlConfig": { "metadata": "@format == 'xml'@", "schema": "string" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_missingFieldForCondition_excludesConditionalProperty() {
        String currentTaskParameters = """
            {
                "name": "test"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "advancedConfig": { "metadata": "@enableAdvanced == true@", "setting": "string (required)" }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_validatesAllLevels() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "metadata": "@level1 == 'enabled'@",
                        "level2": "string (required)",
                        "config2": {
                            "metadata": "@level2 == 'active'@",
                            "level3": "boolean (required)",
                            "config3": {
                                "metadata": "@level3 == true@",
                                "finalValue": "string (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_deepMissingError() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "randomValue": "success"
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "metadata": "",
                        "level2": "string (required)",
                        "config2": {
                            "metadata": "@level2 == 'active'@",
                            "level3": "boolean (required)",
                            "config3": {
                                "metadata": "@level3 == true@",
                                "finalValue": "string (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: config1.config2.config3.finalValue", errors.toString());
        assertEquals("Property 'config1.config2.config3.randomValue' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_deepWarning() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": false,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "metadata": "",
                        "level2": "string (required)",
                        "config2": {
                            "metadata": "@level2 == 'active'@",
                            "level3": "boolean (required)",
                            "config3": {
                                "metadata": "@level3 == true@",
                                "finalValue": "string (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'config1.config2.config3' is not defined in task definition
            Property 'config1.config2.config3.finalValue' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineCondition_excludesRequiredProperty() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": false
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "boolean (required)",
                    "advancedConfig": "string @enableAdvanced == true@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionReverserd_excludesRequiredProperty() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": true,
                "advancedConfig": "value"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "boolean (required)",
                    "advancedConfig": "string @true == enableAdvanced@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredProperty_missingError() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": true
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "boolean (required)",
                    "advancedConfig": "string @enableAdvanced == true@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyInteger_missingError() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4.0
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "float (required)",
                    "advancedConfig": "string @enableAdvanced >= 4@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyReversedFloat_noErrors() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "integer (required)",
                    "advancedConfig": "string @4.1 < enableAdvanced@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyContains_noErrors() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": "true",
                "advancedConfig": "Barabara Strisen"
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "name": "string (required)",
                    "enableAdvanced": "string (required)",
                    "advancedConfig": "string @contains({'Donald_B2rbara','True','true'}, enableAdvanced)@ (required)"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedInlineConditions_validatesAllLevels() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "level2": "string @level1 == 'enabled'@ (required)",
                        "config2": {
                            "level3": "boolean @'active' == level2@ (required)",
                            "config3": {
                                "finalValue": "string @level3 == true@ (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedInlineCondition_missingError() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "config2": {
                        "config3": {
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "level2": "string @level1 == 'enabled'@ (required)",
                        "config2": {
                            "level3": "boolean @'enabled' == level1@ (required)",
                            "config3": {
                                "finalValue": "string @level1 == 'enabled'@ (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: config1.level2
            Missing required property: config1.config2.level3
            Missing required property: config1.config2.config3.finalValue""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedInlineCondition_notMatchingCondition() {
        String currentTaskParameters = """
            {
                "level1": "not_enabled",
                "config1": {
                    "config2": {
                        "config3": {
                        }
                    }
                }
            }
            """;

        String taskDefinition = """
            {
                "parameters": {
                    "level1": "string (required)",
                    "config1": {
                        "level2": "string @level1 == 'enabled'@ (required)",
                        "config2": {
                            "level3": "boolean @'enabled' == level1@ (required)",
                            "config3": {
                                "finalValue": "string @level1 == 'enabled'@ (required)"
                            }
                        }
                    }
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_httpClientPost() {
        String currentTaskParameters = """
            {
                "uri": "https://api.example.com/v1/users",
                "allowUnauthorizedCerts": false,
                "responseType": "JSON",
                "headers": {
                    "Authorization": "Bearer some_fake_token",
                    "Content-Type": "application/json"
                },
                "queryParameters": {
                    "debug": "true"
                },
                "body": {
                    "bodyContentType": "RAW",
                    "bodyContentMimeType": "application/json",
                    "bodyContent": "John Doe"
                },
                "fullResponse": true,
                "followAllRedirects": false,
                "followRedirect": true,
                "ignoreResponseCode": false,
                "proxy": "",
                "timeout": 30000
            }
            """;

        String taskDefinition = """
            {
                "label": "Function of the Action",
                "name": "post_1",
                "type": "httpClient/v1/post",
                "parameters": {
                    "uri": "string (required)",
                    "allowUnauthorizedCerts": "boolean",
                    "responseType": "string",
                    "responseFilename": "string @responseType == 'BINARY'@",
                    "headers": { "metadata": "" },
                    "queryParameters": { "metadata": "" },
                    "body": {
                        "metadata": "",
                        "bodyContentType": "string",
                        "bodyContent": { "metadata": "@body.bodyContentType == 'JSON'@" },
                        "bodyContent": { "metadata": "@body.bodyContentType == 'XML'@" },
                        "bodyContent": { "metadata": "@body.bodyContentType == 'FORM_DATA'@" },
                        "bodyContent": { "metadata": "@body.bodyContentType == 'FORM_URL_ENCODED'@" },
                        "bodyContent": "string @body.bodyContentType == 'RAW'@",
                        "bodyContent": { "metadata": "@body.bodyContentType == 'BINARY'@", "extension": "string (required)", "mimeType": "string (required)", "name": "string (required)", "url": "string (required)" },
                        "bodyContentMimeType": "string @'BINARY' == body.bodyContentType@",
                        "bodyContentMimeType": "string @'RAW' == body.bodyContentType@"
                    },
                    "fullResponse": "boolean",
                    "followAllRedirects": "boolean",
                    "followRedirect": "boolean",
                    "ignoreResponseCode": "boolean",
                    "proxy": "string",
                    "timeout": "integer"
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'headers.Authorization' is not defined in task definition
            Property 'headers.Content-Type' is not defined in task definition
            Property 'queryParameters.debug' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateWorkflowTasks_validTasks_noErrors() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": true,
                        "name": "${testTask1.propString}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {
                        "name": "string (required)",
                        "age": "integer"
                    }
                }
                """,
            "component/v1/action1",
                """
                {
                    "parameters": {
                        "active": "boolean (required)",
                        "name": "string (required)"
                    }
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_invalidTaskStructure_addsError() {
        String tasksJson = """
            [
                {
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {}
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {}
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1",
            new ToolUtils.PropertyInfo("testTask1", "component/v1/action", "", true, true, null, null)
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Missing required field: label", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_invalidTaskParameters_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": 123
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
                """
                {
                    "parameters": {
                        "name": "string (required)",
                        "age": "integer (required)"
                    }
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("""
                Property 'name' has incorrect type. Expected: string, but got: integer
                Missing required property: age""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_noSuchAttributeOutput_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask1.propBool}",
                        "name": "John"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {
                        "name": "string (required)",
                        "age": "integer"
                    }
                }
                """,
            "component/v1/action1",
            """
            {
                "parameters": {
                    "active": "boolean (required)",
                    "name": "string (required)"
                }
            }
            """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'testTask1.propBool' might not exist in the output of 'component/v1/trigger1'", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_wrongDataPillType_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask1.propString}",
                        "name": "John"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {
                        "name": "string (required)",
                        "age": "integer"
                    }
                }
                """,
            "component/v1/action1",
            """
            {
                "parameters": {
                    "active": "boolean (required)",
                    "name": "string (required)"
                }
            }
            """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Property 'testTask1.propString' in output of 'component/v1/trigger1' is of type string, not boolean", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_incorrectOutputOrder_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask3.prepNumber}",
                        "name": "${testTask1.prepString}"
                    }
                },
                {
                    "label": "Test Task 3",
                    "name": "testTask3",
                    "type": "component/v1/action2",
                    "parameters": {
                        "active": "${testTask2.propBool}",
                        "name": "${testTask1.propBool}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
            {
                "parameters": {
                    "name": "string (required)",
                    "age": "integer"
                }
            }
            """,
            "component/v1/action1",
            """
            {
                "parameters": {
                    "active": "boolean (required)",
                    "name": "string (required)"
                }
            }
            """,
            "component/v1/action2",
            """
            {
                "parameters": {
                    "active": "boolean (required)",
                    "name": "string (required)"
                }
            }
            """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Wrong task order: You can't reference 'testTask3.prepNumber' in testTask2.", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_missingTaskDefinition_handlesGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "unknownTask",
                    "type": "component/v1/trigger1",
                    "parameters": {}
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of();

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            // This should throw a NullPointerException or similar when trying to validate
            // parameters with a null task definition, which is expected behavior
            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            // The task structure validation should pass, but parameter validation should fail
            // due to null task definition
            assertFalse(errors.isEmpty(), "Should have errors when task definition is missing");
        } catch (Exception e) {
            // It's acceptable for this to throw an exception when task definition is null
            assertFalse(e.getMessage().isEmpty(),
                "Exception should be related to missing task definition: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_taskWithoutParameters_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {}
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {}
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_taskWithMissingParametersField_handlesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1"
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {}
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertTrue(errors.toString().contains("Missing required field: parameters") ||
                errors.toString().contains("Task definition must have a 'parameters' object"));
        } catch (Exception e) {
            fail("Should handle missing parameters field: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_multipleTasks_validatesAll() {
        String tasksJson = """
            [
                {
                    "label": "Valid Task",
                    "name": "validTask",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "name": "invalidTask",
                    "type": "component/v1/action1",
                    "parameters": {
                        "age": "thirty"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1",
            """
                {
                    "parameters": {
                        "name": "string (required)"
                    }
                }
                """,
            "component/v1/action1",
                """
                {
                    "parameters": {
                        "age": "integer (required)"
                    }
                }
                """);

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            // Should contain errors from both task validation and parameter validation
            assertTrue(errors.toString().contains("Missing required field: label"));
            assertTrue(errors.toString().contains("Property 'age' has incorrect type"));
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_arrayParameters_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "items": ["${task1.propString}", "literal value"]
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"items\": [\"string\"] } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_multipleDataPillsInSameValue_validatesAll() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": true
                    }
                },
                {
                    "label": "Task 3",
                    "name": "task3",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "Name: ${task1.propString}, Other: ${task2.propBool}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"name\": \"string\", \"active\": \"boolean\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            // this is an exception, every value can be converted to string
            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_invalidDataPillFormat_ignoresGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${invalidformat}",
                        "active": "${task1}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"name\": \"string\", \"active\": \"boolean\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            //check this
            assertEquals("Task 'invalidformat' doesn't exits.", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_missingTaskOutputInfo_handlesGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${task1.propString}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"name\": \"string\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of();

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'task1.propString' might not exist in the output of 'component/v1/trigger1'", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_objectDataPillValidation_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${task1.item.propBool}",
                        "height": "${task1.item.propNumber}",
                        "age": "${task1.propInteger}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"active\": \"boolean\", \"height\": \"float\", \"age\": \"integer\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionObj
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_arrayDataPillValidation_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${task1.items[0].propBool}",
                        "height": "${task1.items[0].propNumber}",
                        "age": "${task1.items[0].propInteger}"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"active\": \"boolean\", \"height\": \"float\", \"age\": \"integer\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionArr
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_complexNestedArrayWithDataPills_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "configs": [
                            {
                                "setting": "${task1.propInvalid}",
                                "enabled": true
                            },
                            {
                                "setting": "static value",
                                "enabled": false
                            }
                        ]
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"configs\": \"array\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'task1.propInvalid' might not exist in the output of 'component/v1/trigger1'", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_malformedDataPillFormat_ignoresGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${incomplete",
                        "active": "${}",
                        "value": "normal text"
                    }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "component/v1/trigger1", "{ \"parameters\": { \"name\": \"string\" } }",
            "component/v1/action1", "{ \"parameters\": { \"name\": \"string\", \"active\": \"boolean\", \"value\": \"string\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1
        );

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Property 'active' has incorrect type. Expected: boolean, but got: string", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowCondition_noErrors() {
        String tasksJson = """
            [
                {
                     "label": "Condition",
                     "name": "condition_1",
                     "type": "condition/v1",
                     "parameters": {
                         "rawExpression": false,
                         "caseFalse": [
                            {
                                "label": "Task 1",
                                "name": "task1",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "John"
                                }
                            },
                         ],
                         "caseTrue": [
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "Mike"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, String> taskDefinitions = Map.of(
            "condition/v1", "{ \"parameters\": { \"rawExpression\": \"boolean\" } }",
            "component/v1/action1", "{ \"parameters\": { \"name\": \"string\" } }"
        );

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}
