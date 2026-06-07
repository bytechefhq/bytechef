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
 * Thrown when an attempt is made to create or rename a {@link AiAutoMemory} using a name that already exists for the
 * same (workspaceId, userId) pair.
 *
 * @author Ivica Cardic
 */
public class DuplicateAiAutoMemoryNameException extends RuntimeException {

    private final String name;

    public DuplicateAiAutoMemoryNameException(String name) {
        super("A memory named '" + name + "' already exists for this workspace and user");

        this.name = name;
    }

    public DuplicateAiAutoMemoryNameException(String name, Throwable cause) {
        super("A memory named '" + name + "' already exists for this workspace and user", cause);

        this.name = name;
    }

    public String getName() {
        return name;
    }
}
