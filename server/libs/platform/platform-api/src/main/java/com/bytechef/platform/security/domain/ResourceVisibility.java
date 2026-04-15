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

package com.bytechef.platform.security.domain;

import java.util.Objects;

/**
 * How far a resource reaches. Ordered: PRIVATE &lt; WORKSPACE &lt; ORGANIZATION.
 *
 * <p>
 * This enum is only the vocabulary. Which rungs a given resource type may actually hold, and which one it is created
 * with, are declared per module through {@link ResourceVisibilityPolicy} — connections support all three, while a
 * workspace-bound resource such as a project supports only the first two.
 *
 * <p>
 * Persisted as an INT ordinal by Spring Data JDBC. Appending a value is safe; reordering existing values would
 * reinterpret every persisted row.
 *
 * @author Ivica Cardic
 */
public enum ResourceVisibility {

    PRIVATE, // the owner, plus any users named in a resource grant
    WORKSPACE, // every member of the owning workspace — the default for every resource type
    ORGANIZATION; // every member of every workspace in the organization

    /**
     * Returns whether this visibility reaches at least as far as {@code other}.
     */
    public boolean isAtLeast(ResourceVisibility other) {
        Objects.requireNonNull(other, "other");

        return ordinal() >= other.ordinal();
    }
}
