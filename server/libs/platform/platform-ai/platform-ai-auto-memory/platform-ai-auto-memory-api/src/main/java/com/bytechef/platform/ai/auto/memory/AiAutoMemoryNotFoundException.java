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

package com.bytechef.platform.ai.auto.memory;

/**
 * Thrown by {@link AiAutoMemoryService} when the requested memory row cannot be located within the supplied workspace +
 * user + environment scope. Distinct from {@link DuplicateAiAutoMemoryNameException} (the inverse outcome on the same
 * uniqueness key) so callers can branch cleanly without string-matching messages.
 *
 * @author Ivica Cardic
 */
public class AiAutoMemoryNotFoundException extends RuntimeException {

    public AiAutoMemoryNotFoundException(String message) {
        super(message);
    }
}
