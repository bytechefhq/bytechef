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

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Objects;

/**
 * The scope that owns a set of memories: a principal within one workspace and one environment. Every
 * {@link AiAutoMemoryService} operation on a single owner's memories takes one of these, and a memory belongs to
 * exactly one owner for its whole lifetime. Workspace and principal ids are positive, as every persisted id is.
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryOwner(
    long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, Environment environment) {

    public AiAutoMemoryOwner {
        Objects.requireNonNull(principalType, "principalType");
        Objects.requireNonNull(environment, "environment");

        if (workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId must be positive, got " + workspaceId);
        }

        if (principalId <= 0) {
            throw new IllegalArgumentException("principalId must be positive, got " + principalId);
        }
    }
}
