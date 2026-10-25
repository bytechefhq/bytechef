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

package com.bytechef.cache;

import java.util.Set;

/**
 * Names caches that hold state instead of copies of stored data, so every node of a cluster must see the same entries.
 * Values must be JSON-serializable (strings, numbers, booleans, lists and maps).
 *
 * @author Ivica Cardic
 */
public record SharedCaches(Set<String> cacheNames) {

    public SharedCaches {
        cacheNames = Set.copyOf(cacheNames);
    }

    public static SharedCaches of(String... cacheNames) {
        return new SharedCaches(Set.of(cacheNames));
    }
}
