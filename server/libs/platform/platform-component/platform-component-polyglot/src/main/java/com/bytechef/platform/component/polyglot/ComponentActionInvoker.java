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

package com.bytechef.platform.component.polyglot;

import java.util.Map;

/**
 * Generalization seam consumed by {@link ActionProxyObject}: dispatches a component action invocation coming from a
 * guest polyglot context. Implementations own connection resolution (from {@code connectionName}, which may be
 * {@code null}), component/action lookup, and the actual perform-execution wiring.
 *
 * @author Ivica Cardic
 */
public interface ComponentActionInvoker {

    /**
     * Invokes the named action of the named component with the given (already host-side) input parameters.
     *
     * @param componentName  the name of the component owning the action
     * @param actionName     the name of the action to invoke
     * @param input          the action's input parameters, already converted to host-native values
     * @param connectionName the name of the connection to use, or {@code null} when none was supplied by the guest call
     * @return the result of the action's perform execution, or {@code null}
     * @throws Exception if the invocation fails
     */
    Object invoke(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception;
}
