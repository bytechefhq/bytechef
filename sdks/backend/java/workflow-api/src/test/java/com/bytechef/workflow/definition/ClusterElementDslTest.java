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

package com.bytechef.workflow.definition;

import static com.bytechef.workflow.definition.WorkflowDsl.clusterElement;
import static com.bytechef.workflow.definition.WorkflowDsl.clusterElements;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The builders exist so a Java task composes the same literal a script task writes by hand — so what they build is
 * asserted as plain maps, which is exactly what crosses the Espresso bridge as JSON and what the host parses.
 *
 * @author Ivica Cardic
 */
class ClusterElementDslTest {

    @Test
    void testClusterElementsBuildsTheMapAScriptTaskWouldWriteByHand() {
        Map<String, ?> composed = clusterElements()
            .element(
                "model",
                clusterElement("openAi/v1/model")
                    .connection("openai-prod")
                    .parameter("model", "gpt-4o"))
            .elements(
                "tools",
                clusterElement("slack/v1/sendMessage")
                    .connection("slack-prod")
                    .name("post_to_slack"));

        assertEquals(
            Map.of(
                "model",
                Map.of("type", "openAi/v1/model", "connection", "openai-prod", "parameters",
                    Map.of("model", "gpt-4o")),
                "tools",
                List.of(Map.of("type", "slack/v1/sendMessage", "connection", "slack-prod", "name", "post_to_slack"))),
            composed);
    }

    @Test
    void testClusterElementOmitsWhatWasNotSet() {
        assertEquals(Map.of("type", "openAi/v1/model"), clusterElement("openAi/v1/model"));
    }

    @Test
    void testParametersAccumulateAndCanBeSetWholesale() {
        assertEquals(
            Map.of("type", "openAi/v1/model", "parameters", Map.of("model", "gpt-4o", "temperature", 0.2)),
            clusterElement("openAi/v1/model")
                .parameters(Map.of("model", "gpt-4o"))
                .parameter("temperature", 0.2));
    }

    @Test
    void testTwoElementsOfOneTypeStayInTheOrderTheyWereWritten() {
        Map<String, ?> composed = clusterElements()
            .elements(
                "tools",
                clusterElement("slack/v1/sendMessage").name("post_to_slack"),
                clusterElement("gmail/v1/sendEmail").name("send_email"));

        assertEquals(
            List.of(
                Map.of("type", "slack/v1/sendMessage", "name", "post_to_slack"),
                Map.of("type", "gmail/v1/sendEmail", "name", "send_email")),
            composed.get("tools"));
    }
}
