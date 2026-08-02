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

package com.bytechef.task.dispatcher.graph.constant;

/**
 * @author Ivica Cardic
 */
public class GraphTaskDispatcherConstants {

    public static final String GRAPH = "graph";
    public static final String NODES = "nodes";
    public static final String NAME = "name";
    public static final String TASKS = "tasks";
    public static final String NEXT = "next";
    public static final String START_NODE = "startNode";
    public static final String MAX_TRANSITIONS = "maxTransitions";
    public static final String NODE = "__node";

    /**
     * Distinct sentinel stamped by {@code GraphTaskDispatcher#dispatchRouterNode} for an empty start node, kept
     * separate from {@link #NODE} so that when a graph is itself nested inside another graph's node, the router
     * hand-off never overwrites the outer graph's {@link #NODE} stamp (which {@code MapUtils.append} would do if both
     * used the same key -- the new value wins on a merge).
     */
    public static final String ROUTER_NODE = "__routerNode";

    public static final int DEFAULT_MAX_TRANSITIONS = 100;
}
