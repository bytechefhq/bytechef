/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.domain.Task;
import org.springframework.lang.Nullable;

/**
 * The strategy interface used for resolving the apprpriate {@link TaskDispatcher} instance for a given {@link Task}.
 *
 * @author Arik Cohen
 * @since Mar 26, 2017
 */
public interface TaskDispatcherResolver {
    /**
     * Resolves a {@link TaskDispatcher} for the given {@link Task} instance or <code>null
     * </code> if one can not be resolved.
     *
     * @param task The {@link Task} instance
     * @return a {@link TaskDispatcher} instance to execute the given task or <code>null</code> if unable to resolve
     *         one.
     */
    @Nullable
    TaskDispatcher<? extends Task> resolve(Task task);
}
