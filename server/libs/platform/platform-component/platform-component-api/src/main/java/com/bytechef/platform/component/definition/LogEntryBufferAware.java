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

/**
 * Implemented by contexts that accumulate component log entries in memory instead of writing each one out on its own.
 * Buffering turns the whole-file rewrite that backs a log append into a single write per task execution rather than one
 * per entry.
 *
 * @author Ivica Cardic
 */
public interface LogEntryBufferAware {

    /**
     * Writes out every log entry buffered so far and stops buffering, so that entries logged afterwards are written as
     * they arrive. Called once a task execution has finished with the context; entries logged later still reach
     * storage, which matters for actions whose result is a stream consumed after the perform function returns.
     */
    void flushLogEntries();
}
