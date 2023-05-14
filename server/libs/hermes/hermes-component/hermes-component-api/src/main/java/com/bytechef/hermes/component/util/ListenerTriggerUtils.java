
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

package com.bytechef.hermes.component.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public final class ListenerTriggerUtils {

    static ListenerEmitter listenerEmitter;

    /**
     * @param workflowExecutionId
     * @param output
     */
    @SuppressFBWarnings("UWF")
    public static void emit(String workflowExecutionId, Object output) {
        listenerEmitter.emit(workflowExecutionId, output);
    }

    /**
     *
     */
    interface ListenerEmitter {

        /**
         * @param output
         */
        void emit(String workflowExecutionId, Object output);
    }
}
