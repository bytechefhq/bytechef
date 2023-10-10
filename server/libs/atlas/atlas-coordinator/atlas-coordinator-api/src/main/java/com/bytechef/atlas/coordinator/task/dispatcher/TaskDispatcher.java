
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.coordinator.task.dispatcher;

import com.bytechef.atlas.configuration.task.Task;

/**
 * A strategy interface for dispatching {@link Task} instances to be executed.
 *
 * @author Arik Cohen
 * @since Mar 26, 2017
 */
public interface TaskDispatcher<T extends Task> {
    /**
     * Dispatches a {@link Task} instance.
     *
     * @param task The task to dispatch
     */
    void dispatch(T task);
}
