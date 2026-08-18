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

package com.bytechef.ai.copilot.tool.catalog;

import java.util.List;

/**
 * Contributes one or more {@link IntelligentToolDefinition}s to the {@link IntelligentToolCatalog}. Each activation
 * profile (Copilot panel configuration, AI Hub configuration, management-MCP contributor configuration) that owns an
 * intelligent delegate registers a bean implementing this interface.
 *
 * @author Ivica Cardic
 */
public interface IntelligentToolContributor {

    List<IntelligentToolDefinition> getIntelligentToolDefinitions();
}
