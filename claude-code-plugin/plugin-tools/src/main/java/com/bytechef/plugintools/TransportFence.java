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

package com.bytechef.plugintools;

import java.util.List;

/**
 * One transport-tagged region of a skill document, delimited by a
 * {@code <!-- transport: ... --> ... <!-- /transport -->} fence.
 *
 * @author Ivica Cardic
 */
public record TransportFence(Transport transport, Edition edition, List<String> uses, String body) {

    /**
     * The {@code uses:} token that marks a fence as contributing to the {@code ## always} section of the generated MCP
     * instructions rather than to a per-tool {@code ## tools: ...} section.
     */
    public static final String GENERAL_MARKER = "general";

    public TransportFence {
        uses = List.copyOf(uses);
    }

    /**
     * A fence with no {@code uses} entries, or whose {@code uses} entries include the {@link #GENERAL_MARKER} sentinel,
     * contributes its body to the {@code ## always} section instead of a per-tool section.
     */
    public boolean isGeneral() {
        return uses.isEmpty() || uses.contains(GENERAL_MARKER);
    }
}
