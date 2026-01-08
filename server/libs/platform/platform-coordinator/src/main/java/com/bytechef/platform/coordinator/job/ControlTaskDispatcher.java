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

package com.bytechef.platform.coordinator.job;

import com.bytechef.atlas.configuration.domain.ControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
class ControlTaskDispatcher implements TaskDispatcher<ControlTask>, TaskDispatcherResolver {

    @Override
    public void dispatch(ControlTask controlTask) {
    }

    @Override
    public @Nullable TaskDispatcher<? extends Task> resolve(Task task) {
        if (task instanceof ControlTask) {
            return this;
        }

        return null;
    }
}
