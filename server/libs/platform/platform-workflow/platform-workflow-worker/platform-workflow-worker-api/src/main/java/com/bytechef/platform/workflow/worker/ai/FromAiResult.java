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

package com.bytechef.platform.workflow.worker.ai;

/**
 * Marker object produced by the {@code fromAi} SpEL method. When the evaluator encounters {@code =fromAi('name',
 * 'description', 'type', defaultValue)}, it creates a {@code FromAiResult} that downstream code (e.g. the AI agent
 * action) uses for input-schema generation and runtime value replacement.
 *
 * @param name         the parameter name the AI model will see in the tool schema (required)
 * @param description  human-readable description shown to the AI model (nullable)
 * @param type         data type string: STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT (defaults to STRING)
 * @param defaultValue fallback value when the AI model does not provide one (nullable)
 * @author Ivica Cardic
 */
public record FromAiResult(String name, String description, String type, Object defaultValue) {
}
