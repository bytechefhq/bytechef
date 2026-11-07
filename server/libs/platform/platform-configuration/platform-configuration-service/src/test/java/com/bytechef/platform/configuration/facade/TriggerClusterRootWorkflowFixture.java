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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;

/**
 * @author Ivica Cardic
 */
final class TriggerClusterRootWorkflowFixture {

    static final String WORKFLOW_ID = "workflow1";
    static final String TRIGGER_NAME = "trigger_1";
    static final String VOICE_AGENT_TYPE_NAME = "voiceAgent";
    static final String VOICE_AGENT_NAME = "voiceAgent_1";
    static final String TOOLS_TYPE_NAME = "tools";
    static final String TOOL_NAME = "getOrder_1";

    static final String DEFINITION = """
        {
            "triggers": [
                {
                    "name": "trigger_1",
                    "type": "browser/v1/voiceSession",
                    "parameters": {"greeting": "Hello"},
                    "clusterElements": {
                        "voiceAgent": {
                            "name": "voiceAgent_1",
                            "type": "deepgram/v1/voiceAgent",
                            "parameters": {"model": "nova-2"}
                        },
                        "tools": [
                            {
                                "name": "getOrder_1",
                                "type": "shopify/v1/getOrder",
                                "parameters": {"orderId": "42"}
                            }
                        ]
                    }
                }
            ],
            "tasks": [
                {
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "done"}
                }
            ]
        }
        """;

    private TriggerClusterRootWorkflowFixture() {
    }

    static Workflow workflow() {
        return new Workflow(WORKFLOW_ID, DEFINITION, Format.JSON);
    }
}
