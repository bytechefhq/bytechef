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
 * Thrown by {@link AiAutoMemoryService} when a memory changed — or was deleted — between the moment the caller read it
 * and the moment its write landed. The write is not applied: the caller has to read the memory again and redo its edit
 * against the current content, instead of overwriting a change it never saw. The message is written for the agent's
 * memory tools; a caller addressing a person builds its own from {@link #getName()}.
 *
 * @author Ivica Cardic
 */
public class AiAutoMemoryConcurrentModificationException extends RuntimeException {

    private final String name;

    public AiAutoMemoryConcurrentModificationException(String name) {
        this(name, null);
    }

    public AiAutoMemoryConcurrentModificationException(String name, Throwable cause) {
        super(
            "Memory '" + name + "' was changed by someone else while this operation ran. View it again and retry the "
                + "operation.",
            cause);

        this.name = name;
    }

    public String getName() {
        return name;
    }
}
