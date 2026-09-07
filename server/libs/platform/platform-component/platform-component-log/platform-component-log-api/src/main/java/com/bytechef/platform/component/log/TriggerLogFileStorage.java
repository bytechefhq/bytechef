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

package com.bytechef.platform.component.log;

/**
 * Storage for the logs a trigger writes while it runs, keyed by trigger execution rather than by job: a trigger runs
 * before any job exists and may produce none. A sibling of {@link LogFileStorage}, not a subtype, so each
 * {@code LogFileStorage}-typed injection point still sees exactly one candidate.
 *
 * @author Ivica Cardic
 */
public interface TriggerLogFileStorage extends LogFileStorageReader, LogFileStorageWriter {
}
