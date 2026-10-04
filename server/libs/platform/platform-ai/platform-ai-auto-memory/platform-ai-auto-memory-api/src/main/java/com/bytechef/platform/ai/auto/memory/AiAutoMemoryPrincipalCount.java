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

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public record AiAutoMemoryPrincipalCount(
    AiAutoMemoryPrincipalType principalType, long principalId, int memoryCount) {

    public AiAutoMemoryPrincipalCount {
        Objects.requireNonNull(principalType, "principalType");

        if (principalId <= 0) {
            throw new IllegalArgumentException("principalId must be positive, got " + principalId);
        }

        if (memoryCount < 0) {
            throw new IllegalArgumentException("memoryCount must not be negative, got " + memoryCount);
        }
    }
}
