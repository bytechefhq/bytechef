
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.task.execution;

/**
 * Defines the various states that a {@link com.bytechef.atlas.domain.TaskExecution} can be in at any give moment in
 * time.
 *
 * @author Arik Cohen
 */
public enum TaskStatus {
    CREATED(false),
    STARTED(false),
    FAILED(true),
    CANCELLED(true),
    COMPLETED(true);

    private final boolean terminated;

    TaskStatus(boolean terminated) {
        this.terminated = terminated;
    }

    public boolean isTerminated() {
        return terminated;
    }
}
