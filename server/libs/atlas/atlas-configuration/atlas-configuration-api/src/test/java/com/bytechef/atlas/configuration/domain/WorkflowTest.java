/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.configuration.domain;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WorkflowTest {

    private static final String DEFINITION = """
        {
            "label" : "v2-loop-condition",
            "description" : "",
            "inputs" : [ ],
            "tasks" : [ {
                "label" : "Google Mail",
                "name" : "googleMail_1",
                "parameters" : {
                    "format" : "simple",
                    "id" : "${trigger_1.id}"
                },
                "type" : "googleMail/v1/getMail",
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : { }
                    }
                }
            }, {
                "label" : "OpenAI",
                "name" : "openai_1",
                "parameters" : {
                    "responseFormat" : 1,
                    "n" : 1,
                    "temperature" : 1,
                    "topP" : 1,
                    "frequencyPenalty" : 0,
                    "presencePenalty" : 0,
                    "model" : "gpt-4",
                    "messages" : [ {
                        "content" : "...",
                        "role" : "user"
                    }, {
                        "role" : "system",
                        "content" : "..."
                    } ]
                },
                "type" : "openai/v1/ask",
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : { }
                    }
                }
            }, {
                "label" : "Google Calendar",
                "name" : "googleCalendar_1",
                "type" : "googleCalendar/v1/getFreeTimeSlots",
                "parameters" : {
                    "maxResults" : 250,
                    "calendarId" : "bytechefconnectordev@gmail.com",
                    "dateRange" : {
                        "from" : "${openai_1.dateTimePeriod.startDateTime}",
                        "to" : "${openai_1.dateTimePeriod.endDateTime}"
                    }
                },
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : { }
                    }
                }
            }, {
                "type" : "loop/v1",
                "name" : "loop_1",
                "parameters" : {
                    "items" : "${googleCalendar_1}",
                    "iteratee" : [ {
                        "label" : "If a time slot is longer than 45 minutes",
                        "name" : "condition_1",
                        "type" : "condition/v1",
                        "parameters" : {
                            "rawExpression" : true,
                            "expression" : "...",
                            "caseTrue" : [ {
                                "label" : "Data Storage",
                                "name" : "dataStorage_1",
                                "parameters" : {
                                    "scope" : "CURRENT_EXECUTION",
                                    "key" : "validTimeSlots",
                                    "type" : 8,
                                    "value" : "${loop_1.item}"
                                },
                                "type" : "dataStorage/v1/appendValueToList",
                                "metadata" : {
                                    "ui" : {
                                        "dynamicPropertyTypes" : { }
                                    }
                                }
                            } ],
                            "caseFalse" : [ {
                                "label" : "Log Warning",
                                "name" : "logEntryWarn1",
                                "parameters" : {
                                    "text" : " ${loop_1.item} is NOT valid"
                                },
                                "type" : "logger/v1/warn"
                            } ]
                        }
                    } ]
                }
            }, {
                "label" : "Data Storage",
                "name" : "dataStorage_2",
                "type" : "dataStorage/v1/getValue",
                "parameters" : {
                    "key" : "validTimeSlots",
                    "scope" : "CURRENT_EXECUTION",
                    "type" : 8,
                    "defaultValue" : {
                        "startTime" : "2024-09-15T08:29",
                        "endTime" : "2024-09-16T08:29"
                    }
                },
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : {
                            "defaultValue.startTime" : "DATE_TIME",
                            "defaultValue.endTime" : "DATE_TIME"
                        }
                    }
                }
            }, {
                "label" : "OpenAI",
                "name" : "openai_2",
                "parameters" : {
                    "responseFormat" : 0,
                    "n" : 1,
                    "temperature" : 1,
                    "topP" : 1,
                    "frequencyPenalty" : 0,
                    "presencePenalty" : 0,
                    "model" : "gpt-4",
                    "messages" : [ {
                        "content" : "...",
                        "role" : "user"
                    } ]
                },
                "type" : "openai/v1/ask",
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : { }
                    }
                }
            }, {
                "label" : "Google Mail",
                "name" : "googleMail_2",
                "type" : "googleMail/v1/replyToEmail",
                "parameters" : {
                    "body" : "...",
                    "id" : "${googleMail_1.id}",
                    "bcc" : "${googleMail_1.bcc}",
                    "cc" : "${googleMail_1.cc}",
                    "to" : [ "${googleMail_1.from}" ]
                },
                "metadata" : {
                    "ui" : {
                        "dynamicPropertyTypes" : {
                            "to[0]" : "STRING"
                        }
                    }
                }
            } ]
        }
        """;

    @Test
    public void testGetAllTasks() {
        Workflow workflow = new Workflow(DEFINITION, Workflow.Format.JSON);

        List<WorkflowTask> workflowTasks = workflow.getAllTasks();

        Assertions.assertEquals(8, workflowTasks.size());

        // TODO
    }
}
