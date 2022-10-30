/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.definition.Action;
import com.bytechef.hermes.component.exception.ActionExecutionException;

/**
 * @author Ivica Cardic
 */
public interface GenericComponentHandler extends ComponentHandler {

    /**
     * This can be useful if we still want to have only one method to handle all operations
     *
     * @param action
     * @param context
     * @param executionParameters
     * @return
     * @throws ActionExecutionException
     */
    Object handle(Action action, Context context, ExecutionParameters executionParameters)
            throws ActionExecutionException;
}
