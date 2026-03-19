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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Marko Kriskovic
 */
class WorkflowValidatorClusterElementsTest {

    private static final PropertyInfo trigger1 =
        new PropertyInfo("propString", "STRING", null, false, true, null, null);
    private static final PropertyInfo action1 =
        new PropertyInfo("propBool", "BOOLEAN", null, false, true, null, null);

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void validateWorkflowTasksClusterElementsMissingRequiredProperty() {
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
                    "clusterElements": {
                        "model": {
                            "label": "OpenAI",
                            "name": "openAi_2",
                            "parameters": {
                                "model": "gpt-5-nano"
                            },
                        "type": "openAi/v1/model"
                        },
                        "chatMemory": null,
                        "rag": {
                           "clusterElements": {
                               "vectorStore": {
                                   "clusterElements": {
                                       "embedding": {
                                           "label": "OpenAI",
                                           "name": "openAi_3",
                                           "parameters": {
                                                "temperature": 0.5
                                           },
                                           "type": "openAi/v1/embedding"
                                       }
                                   },
                                   "label": "Couchbase",
                                   "name": "couchbase_1",
                                   "parameters": {},
                                   "type": "couchbase/v1/vectorStore"
                               }
                           },
                           "label": "Question Answer RAG",
                           "name": "questionAnswerRag_1",
                           "parameters": {
                               "similarityThreshold": 0,
                               "topK": 4
                           },
                           "type": "questionAnswerRag/v1/rag"
                       },
                        "guardrails": null,
                        "tools": []
                    },
                    "name": "aiAgent_1",
                    "label": "AI Agent",
                    "parameters": {
                        "userPrompt": "${trigger_1.propString}"
                    },
                    "type": "aiAgent/v1/chat"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "questionAnswerRag/v1/rag", List.of(
                new PropertyInfo("similarityThreshold", "INTEGER", null, false, true, null, null),
                new PropertyInfo("topK", "INTEGER", null, false, true, null, null)),
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "openAi/v1/embedding", List.of(
                new PropertyInfo("model", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, new HashMap<>(),
                errors, warnings);

            assertEquals("Missing required property: aiAgent_1.questionAnswerRag_1.couchbase_1.openAi_3.model",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksClusterElementsDifferentTypeProperty() {
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
                    "clusterElements": {
                        "model": {
                            "label": "OpenAI",
                            "name": "openAi_2",
                            "parameters": {
                                "model": "gpt-5-nano"
                            },
                        "type": "openAi/v1/model"
                        },
                        "chatMemory": null,
                        "rag": {
                           "clusterElements": {
                               "vectorStore": {
                                   "clusterElements": {
                                       "embedding": {
                                           "label": "OpenAI",
                                           "name": "openAi_3",
                                           "parameters": {
                                                "model": 0.5
                                           },
                                           "type": "openAi/v1/embedding"
                                       }
                                   },
                                   "label": "Couchbase",
                                   "name": "couchbase_1",
                                   "parameters": {},
                                   "type": "couchbase/v1/vectorStore"
                               }
                           },
                           "label": "Question Answer RAG",
                           "name": "questionAnswerRag_1",
                           "parameters": {
                               "similarityThreshold": 0,
                               "topK": 4
                           },
                           "type": "questionAnswerRag/v1/rag"
                       },
                        "guardrails": null,
                        "tools": []
                    },
                    "name": "aiAgent_1",
                    "label": "AI Agent",
                    "parameters": {
                        "userPrompt": "${trigger_1.propString}"
                    },
                    "type": "aiAgent/v1/chat"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "questionAnswerRag/v1/rag", List.of(
                new PropertyInfo("similarityThreshold", "INTEGER", null, false, true, null, null),
                new PropertyInfo("topK", "INTEGER", null, false, true, null, null)),
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "openAi/v1/embedding", List.of(
                new PropertyInfo("model", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, new HashMap<>(),
                errors, warnings);

            assertEquals(
                "Property 'aiAgent_1.questionAnswerRag_1.couchbase_1.openAi_3.model' has incorrect type. Expected: string, but got: number",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksClusterElementsNoSuchClusterElement() {
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
                    "clusterElements": {
                        "model": {
                            "label": "OpenAI",
                            "name": "openAi_2",
                            "parameters": {
                                "model": "gpt-5-nano"
                            },
                        "type": "openAi/v1/model"
                        },
                        "chatMemory": null,
                        "rag": {
                           "clusterElements": {
                               "vectorStore": {
                                   "clusterElements": {
                                       "embedding": {
                                           "label": "OpenAI",
                                           "name": "openAi_3",
                                           "parameters": {
                                                "model": "gpt-5-nano"
                                           },
                                           "type": "openAi/v1/embedding"
                                       }
                                   },
                                   "label": "Couchbase",
                                   "name": "couchbase_1",
                                   "parameters": {},
                                   "type": "couchbase/v1/vectorStore"
                               }
                           },
                           "label": "Question Answer RAG",
                           "name": "questionAnswerRag_1",
                           "parameters": {
                               "similarityThreshold": 0,
                               "topK": 4
                           },
                           "type": "questionAnswerRag/v1/rag"
                       },
                        "guardsnails": null,
                        "tools": []
                    },
                    "name": "aiAgent_1",
                    "label": "AI Agent",
                    "parameters": {
                        "userPrompt": "${trigger_1.propString}"
                    },
                    "type": "aiAgent/v1/chat"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "questionAnswerRag/v1/rag", List.of(
                new PropertyInfo("similarityThreshold", "INTEGER", null, false, true, null, null),
                new PropertyInfo("topK", "INTEGER", null, false, true, null, null)),
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "openAi/v1/embedding", List.of(
                new PropertyInfo("model", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        Map<String, List<String>> clusterElements = Map.of(
            "aiAgent/v1/chat", List.of("model", "chatMemory", "rag", "guardrails", "tools"),
            "openAi/v1/embedding", List.of(),
            "couchbase/v1/vectorStore", List.of("embedding"));

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, clusterElements,
                errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("""
                Cluster element 'guardrails' is missing from task aiAgent_1
                Cluster element 'guardsnails' are not defined in task aiAgent_1""", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksClusterElementsNestedNoSuchClusterElement() {
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
                    "clusterElements": {
                        "model": {
                            "label": "OpenAI",
                            "name": "openAi_2",
                            "parameters": {
                                "model": "gpt-5-nano"
                            },
                        "type": "openAi/v1/model"
                        },
                        "chatMemory": null,
                        "rag": {
                           "clusterElements": {
                               "vectorStore": {
                                   "clusterElements": {
                                       "model": {
                                           "label": "OpenAI",
                                           "name": "openAi_3",
                                           "parameters": {
                                                "model": "gpt-5-nano"
                                           },
                                           "type": "openAi/v1/embedding"
                                       }
                                   },
                                   "label": "Couchbase",
                                   "name": "couchbase_1",
                                   "parameters": {},
                                   "type": "couchbase/v1/vectorStore"
                               }
                           },
                           "label": "Question Answer RAG",
                           "name": "questionAnswerRag_1",
                           "parameters": {
                               "similarityThreshold": 0,
                               "topK": 4
                           },
                           "type": "questionAnswerRag/v1/rag"
                       },
                        "guardrails": null,
                        "tools": []
                    },
                    "name": "aiAgent_1",
                    "label": "AI Agent",
                    "parameters": {
                        "userPrompt": "${trigger_1.propString}"
                    },
                    "type": "aiAgent/v1/chat"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "questionAnswerRag/v1/rag", List.of(
                new PropertyInfo("similarityThreshold", "INTEGER", null, false, true, null, null),
                new PropertyInfo("topK", "INTEGER", null, false, true, null, null)),
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "openAi/v1/embedding", List.of(
                new PropertyInfo("model", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        Map<String, List<String>> clusterElements = Map.of(
            "aiAgent/v1/chat", List.of("model", "chatMemory", "rag", "guardrails", "tools"),
            "openAi/v1/embedding", List.of(),
            "couchbase/v1/vectorStore", List.of("embedding"));

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, clusterElements,
                errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("""
                Cluster element 'embedding' is missing from task couchbase_1
                Cluster element 'model' are not defined in task couchbase_1""", warnings.toString());

        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksClusterElementsInConditionNoError() {
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
                     "label": "Condition",
                     "name": "condition_1",
                     "parameters": {
                         "rawExpression": false,
                         "expression": "=",
                         "caseFalse": [
                             {
                                 "label": "AI Agent",
                                 "name": "aiAgent_1",
                                 "parameters": {
                                     "userPrompt": "hi"
                                 },
                                 "clusterElements": {},
                                 "type": "aiAgent/v1/chat"
                             }
                         ],
                         "caseTrue": []
                     },
                     "type": "condition/v1"
                 }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "condition/v1", List.of(
                new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        Map<String, List<String>> clusterElements = Map.of(
            "aiAgent/v1/chat", List.of("model", "chatMemory", "rag", "guardrails", "tools"));

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, clusterElements,
                errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("""
                Property 'expression' is not defined in task definition
                Cluster element 'model' is missing from task aiAgent_1
                Cluster element 'chatMemory' is missing from task aiAgent_1
                Cluster element 'rag' is missing from task aiAgent_1
                Cluster element 'guardrails' is missing from task aiAgent_1
                Cluster element 'tools' is missing from task aiAgent_1""", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowClusterElementsNoErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/trigger1",
                        "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "tasks": [
                    {
                        "clusterElements": {
                            "model": {
                                "label": "OpenAI",
                                "name": "openAi_2",
                                "parameters": {
                                    "model": "gpt-5-nano"
                                },
                            "type": "openAi/v1/model"
                            },
                            "chatMemory": null,
                            "rag": {
                               "clusterElements": {
                                   "vectorStore": {
                                       "clusterElements": {
                                           "embedding": {
                                               "label": "OpenAI",
                                               "name": "openAi_3",
                                               "parameters": {
                                                    "model": "text-embedding-3-small"
                                               },
                                               "type": "openAi/v1/embedding"
                                           }
                                       },
                                       "label": "Couchbase",
                                       "name": "couchbase_1",
                                       "parameters": {},
                                       "type": "couchbase/v1/vectorStore"
                                   }
                               },
                               "label": "Question Answer RAG",
                               "name": "questionAnswerRag_1",
                               "parameters": {
                                   "similarityThreshold": 0,
                                   "topK": 4
                               },
                               "type": "questionAnswerRag/v1/rag"
                           },
                            "guardrails": null,
                            "tools": [
                                {
                                    "label": "Test 2",
                                    "name": "testTask2",
                                    "type": "component/v1/action1",
                                    "parameters": {
                                        "name": "John"
                                    }
                                },
                                {
                                    "label": "Test 3",
                                    "name": "testTask3",
                                    "type": "component/v1/action2",
                                    "parameters": {
                                        "name": "Bruno"
                                    }
                                }
                            ]
                        },
                        "name": "aiAgent_1",
                        "label": "AI Agent",
                        "parameters": {
                            "userPrompt": "${trigger_1.propString}"
                        },
                        "type": "aiAgent/v1/chat"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "questionAnswerRag/v1/rag", List.of(
                new PropertyInfo("similarityThreshold", "INTEGER", null, false, true, null, null),
                new PropertyInfo("topK", "INTEGER", null, false, true, null, null)),
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "openAi/v1/embedding", List.of(
                new PropertyInfo("model", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        Map<String, List<String>> expectedClusterTypesMap = Map.of(
            "aiAgent/v1/chat", List.of("model", "chatMemory", "rag", "guardrails", "tools"),
            "questionAnswerRag/v1/rag", List.of("vectorStore"),
            "couchbase/v1/vectorStore", List.of("embedding"));

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider =
            (taskType) -> expectedClusterTypesMap.get(taskType);

        Map<String, List<String>> clusterElementTypesMap = new HashMap<>();

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), clusterElementTypesMap, errors, warnings);

        assertEquals(
            "{questionAnswerRag/v1/rag=[vectorStore], aiAgent/v1/chat=[model, chatMemory, rag, guardrails, tools], couchbase/v1/vectorStore=[embedding]}",
            clusterElementTypesMap.toString());
        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowClusterElementsInCondition() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/trigger1",
                        "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "tasks": [
                    {
                        "label": "Condition",
                        "name": "condition_1",
                        "parameters": {
                            "rawExpression": false,
                            "expression": "=",
                            "caseFalse": [
                                {
                                    "label": "AI Agent",
                                    "name": "aiAgent_1",
                                    "parameters": {
                                        "userPrompt": "hi"
                                    },
                                    "clusterElements": {},
                                    "type": "aiAgent/v1/chat"
                                }
                            ],
                            "caseTrue": []
                        },
                        "type": "condition/v1"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "condition/v1", List.of(
                new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "aiAgent/v1/chat", action1);

        Map<String, List<String>> expectedClusterTypesMap = Map.of(
            "aiAgent/v1/chat", List.of("model", "chatMemory", "rag", "guardrails", "tools"));

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider =
            (taskType) -> expectedClusterTypesMap.get(taskType);

        Map<String, List<String>> clusterElementTypesMap = new HashMap<>();

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), clusterElementTypesMap, errors, warnings);

        assertEquals("{aiAgent/v1/chat=[model, chatMemory, rag, guardrails, tools]}",
            clusterElementTypesMap.toString());
        assertEquals("", errors.toString());
        assertEquals("""
            Property 'expression' is not defined in task definition
            Cluster element 'model' is missing from task aiAgent_1
            Cluster element 'chatMemory' is missing from task aiAgent_1
            Cluster element 'rag' is missing from task aiAgent_1
            Cluster element 'guardrails' is missing from task aiAgent_1
            Cluster element 'tools' is missing from task aiAgent_1""", warnings.toString());
    }
}
