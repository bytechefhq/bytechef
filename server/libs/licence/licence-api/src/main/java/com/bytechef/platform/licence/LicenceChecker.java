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

package com.bytechef.platform.licence;

/**
 * Provides licence checks for task execution limits.
 *
 * Implementations are responsible for determining how many task executions are allowed by the current licence. A
 * negative value (e.g., -1) can be used to represent "unlimited".
 */
public interface LicenceChecker {

    /**
     * Returns the number of allowed task executions for the current licence.
     *
     * @return the allowed number of task executions, or a negative value for unlimited
     */
    int getAllowedTasks();
}
