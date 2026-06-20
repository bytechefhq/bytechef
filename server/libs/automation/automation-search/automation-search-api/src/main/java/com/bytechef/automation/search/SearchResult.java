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

package com.bytechef.automation.search;

import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface SearchResult<ID> {

    ID id();

    String name();

    String description();

    SearchAssetType type();

    /**
     * The id of the workspace this result belongs to, used by {@code AutomationSearchFacade} to drop results from
     * workspaces the caller cannot access. Returns {@code null} for providers that have not yet been workspace-scoped
     * (those results are passed through unchanged during the incremental rollout) and for genuinely
     * workspace-independent results.
     */
    default @Nullable Long workspaceId() {
        return null;
    }
}
