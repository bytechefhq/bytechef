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

package com.bytechef.ai.mcp.tool.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * Conditional annotation that activates beans when either MCP server or AI Copilot is enabled.
 *
 * <p>
 * This condition matches when any of the following properties is set to {@code true}:
 * <ul>
 * <li>{@code bytechef.ai.mcp.server.enabled}</li>
 * <li>{@code bytechef.ai.copilot.enabled}</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
@Target({
    ElementType.TYPE, ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnAiEnabledCondition.class)
public @interface ConditionalOnAiEnabled {
}
