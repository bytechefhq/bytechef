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

/**
 * The ASK/BUILD axis an intelligent delegate tool is registered on. Not every definition offers both variants — for
 * example the converter delegate exists only in BUILD — so a definition reports a {@code null}
 * {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} for the variant it does not support, and
 * the catalog skips it for that variant.
 *
 * @author Ivica Cardic
 */
public enum IntelligentToolVariant {

    ASK, BUILD
}
