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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface JobContextAware {

    /**
     * Converts the provided component information and connection details into an {@link ActionContext} instance.
     *
     * @param componentName       the name of the component associated with the action context.
     * @param componentVersion    the version of the component.
     * @param actionName          the name of the action being executed.
     * @param componentConnection an optional {@link ComponentConnection} containing connection details for the
     *                            component; can be null.
     * @return an instance of {@link ActionContext} representing the provided information and connection details.
     */
    ActionContext toActionContext(
        String componentName, int componentVersion, String actionName,
        @Nullable ComponentConnection componentConnection);

    /**
     * Retrieves the unique identifier for the environment.
     *
     * @return the environment ID as a {@link Long}, or {@code null} if the ID is not available.
     */
    @Nullable
    Long getEnvironmentId();
}
