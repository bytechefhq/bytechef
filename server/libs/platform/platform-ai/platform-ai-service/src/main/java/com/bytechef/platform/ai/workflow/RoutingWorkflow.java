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

package com.bytechef.platform.ai.workflow;

import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.Assert;

/**
 * Implements the Routing workflow pattern that classifies input and directs it to specialized followup tasks. This
 * workflow enables separation of concerns by routing different types of inputs to specialized prompts and processes
 * optimized for specific categories.
 *
 * <p>
 * The routing workflow is particularly effective for complex tasks where:
 * <ul>
 * <li>There are distinct categories of input that are better handled separately</li>
 * <li>Classification can be handled accurately by an LLM or traditional classification model</li>
 * <li>Different types of input require different specialized processing or expertise</li>
 * </ul>
 *
 * <p>
 * Common use cases include:
 * <ul>
 * <li>Customer support systems routing different types of queries (billing, technical, etc.)</li>
 * <li>Content moderation systems routing content to appropriate review processes</li>
 * <li>Query optimization by routing simple/complex questions to different model capabilities</li>
 * </ul>
 *
 * <p>
 * This implementation allows for dynamic routing based on content classification, with each route having its own
 * specialized prompt optimized for specific types of input.
 * <p/>
 *
 * Implementation uses the
 * <a href= "https://docs.spring.io/spring-ai/reference/1.0/api/structured-output-converter.html">Spring AI Structure
 * Output</a> to convert the chat client response into a structured {@link RoutingResponse} object.
 *
 * @see org.springframework.ai.chat.client.ChatClient
 * @see <a href= "https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html">Spring AI ChatClient</a>
 * @see <a href= "https://www.anthropic.com/research/building-effective-agents">Building Effective Agents</a>
 * @see <a href= "https://docs.spring.io/spring-ai/reference/1.0/api/structured-output-converter.html">Spring AI
 *      Structure Output</a>
 *
 * @author Christian Tzolov
 * @author Marko Kriskovic
 */
public class RoutingWorkflow {

    private static final String DEFAULT_ROUTER_PROMPT =
        """
            Analyze the input content and determine the most appropriate route based on content classification.
            The classification process considers key terms, context and patterns in the input to select the optimal route.

            Input:
            {input}

            Available routes:
            {routes}

            Provide your selection in this JSON format:
            \\{
                "reasoning": "The reasoning behind the route selection, explaining why this particular route was chosen based on the input analysis.",
                "selection": "The chosen category"
            \\}
            """;

    private final ChatClient chatClient;

    public RoutingWorkflow(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Routes input to a specialized prompt based on content classification. This method first analyzes the input to
     * determine the most appropriate route, then processes the input using the specialized prompt for that route.
     *
     * <p>
     * The routing process involves:
     * <ol>
     * <li>Content analysis to determine the appropriate category</li>
     * <li>Selection of a specialized prompt optimized for that category</li>
     * <li>Processing the input with the selected prompt</li>
     * </ol>
     *
     * <p>
     * This approach allows for:
     * <ul>
     * <li>Better handling of diverse input types</li>
     * <li>Optimization of prompts for specific categories</li>
     * <li>Improved accuracy through specialized processing</li>
     * </ul>
     *
     * @param input  The input text to be routed and processed
     * @param routes Map of route names to their corresponding specialized prompts
     * @return selected route
     */
    public String route(String input, Map<String, String> routes) {
        Assert.notNull(input, "Input text cannot be null");
        Assert.notEmpty(routes, "Routes map cannot be null or empty");

        return determineRoute(input, routes.keySet());
    }

    /**
     * Analyzes the input content and determines the most appropriate route based on content classification. The
     * classification process considers key terms, context, and patterns in the input to select the optimal route.
     *
     * <p>
     * The method uses an LLM to:
     * <ul>
     * <li>Analyze the input content and context</li>
     * <li>Consider the available routing options</li>
     * <li>Provide reasoning for the routing decision</li>
     * <li>Select the most appropriate route</li>
     * </ul>
     *
     * @param input           The input text to analyze for routing
     * @param availableRoutes The set of available routing options
     * @return The selected route key based on content analysis
     */
    @SuppressWarnings("null")
    private String determineRoute(String input, Iterable<String> availableRoutes) {

        RoutingResponse routingResponse = chatClient.prompt()
            .user(u -> u.text(DEFAULT_ROUTER_PROMPT)
                .param("routes", availableRoutes.toString())
                .param("input", input))
            .call()
            .entity(RoutingResponse.class);

        Assert.notNull(routingResponse, "Router response must not be null");

        return routingResponse.selection();
    }

    /**
     * Record representing the response from the routing classification process.
     *
     * <p>
     * This record is used by the {@link RoutingWorkflow} to capture and communicate routing decisions made by the LLM
     * classifier.
     *
     * @param reasoning A detailed explanation of why a particular route was chosen, considering factors like key terms,
     *                  user intent, and urgency level
     * @param selection The name of the selected route that will handle the input
     */
    private record RoutingResponse(String reasoning, String selection) {
    }
}
