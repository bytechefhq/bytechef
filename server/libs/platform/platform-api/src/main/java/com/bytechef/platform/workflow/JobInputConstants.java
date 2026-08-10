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

package com.bytechef.platform.workflow;

/**
 * Reserved job-input keys the platform seeds alongside user/trigger inputs. Reserved keys use a double-underscore
 * prefix; workflow input names with that prefix are rejected at validation time so user inputs can never collide.
 *
 * @author Ivica Cardic
 */
public final class JobInputConstants {

    /**
     * Name of the trigger node that fired this job, seeded at every job-creation path that seeds a trigger output. Lets
     * downstream expressions (e.g. an agent workflow's branch dispatcher) ask "which trigger fired" portably.
     */
    public static final String TRIGGER_NAME_INPUT = "__triggerName";

    private JobInputConstants() {
    }
}
