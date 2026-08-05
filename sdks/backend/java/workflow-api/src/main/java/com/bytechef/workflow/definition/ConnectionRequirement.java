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

import java.util.OptionalInt;

/**
 * Declares a connection a {@link TaskDefinition} uses when its perform invokes component actions through
 * {@link TaskContext#component}. The declaration makes a code workflow's connection requirements readable from its
 * generated definition; at runtime the {@link #getName() name} is the same value the perform code passes as
 * {@code connectionName}, resolved against the connection store for the current environment by that name.
 *
 * <p>
 * Declarations are advisory — an undeclared component invocation still executes; declaring makes the requirement
 * introspectable and eligible for deploy-time validation.
 *
 * @author Ivica Cardic
 */
public interface ConnectionRequirement {

    /**
     * Returns the name of the component the connection is used with.
     *
     * @return the component name
     */
    String getComponentName();

    /**
     * Returns the component version the declaration pins, if any. An empty value means the latest component version,
     * matching how the perform-time dispatch resolves components.
     *
     * @return the pinned component version, or {@link OptionalInt#empty()} for latest
     */
    OptionalInt getComponentVersion();

    /**
     * Returns the connection name — the value the perform code passes as {@code connectionName}, and the
     * Connections-page name it resolves to in the execution environment.
     *
     * @return the connection name
     */
    String getName();
}
