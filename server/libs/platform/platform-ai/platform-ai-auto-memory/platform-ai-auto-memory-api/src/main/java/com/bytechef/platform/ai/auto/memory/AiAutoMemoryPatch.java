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

import org.jspecify.annotations.Nullable;

/**
 * The fields a partial update changes. A {@code null} field keeps the stored value; a blank {@code description} clears
 * it, while a blank {@code title} or {@code content} is rejected by the service, since both are required. At least one
 * field must be given.
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryPatch(
    @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType,
    @Nullable String content) {

    public AiAutoMemoryPatch {
        if (title == null && description == null && memoryType == null && content == null) {
            throw new IllegalArgumentException(
                "At least one of title, description, memoryType, content must be provided");
        }
    }
}
