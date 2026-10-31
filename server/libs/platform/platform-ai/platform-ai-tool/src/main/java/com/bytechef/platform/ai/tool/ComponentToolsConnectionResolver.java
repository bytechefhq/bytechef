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

package com.bytechef.platform.ai.tool;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Picks the connection {@link ComponentTools} uses to run an action whose output is only known after a test run. The
 * module that owns the caller's connections contributes the bean, so it decides which connections the caller may use;
 * without a bean there is no fallback.
 *
 * @author Ivica Cardic
 */
public interface ComponentToolsConnectionResolver {

    /**
     * @return the id of a connection of the component the caller described by the tool context may use, or empty when
     *         there is none
     */
    Optional<Long> resolveConnectionId(String componentName, int componentVersion, @Nullable ToolContext toolContext);
}
