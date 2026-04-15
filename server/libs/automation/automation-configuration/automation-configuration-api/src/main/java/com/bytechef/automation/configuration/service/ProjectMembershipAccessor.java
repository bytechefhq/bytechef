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

package com.bytechef.automation.configuration.service;

import java.util.List;

/**
 * Filters a list of workspace project IDs down to those the given user is a member of. CE has no per-project membership
 * concept (every workspace member implicitly sees every project), so the CE implementation returns the input list
 * unchanged. EE wires a narrower implementation backed by {@code ProjectUserService}, which closes the
 * PROJECT-visibility gap where a connection shared to a project would otherwise be visible to any workspace member —
 * not just members of that project.
 *
 * @author Ivica Cardic
 */
public interface ProjectMembershipAccessor {

    /**
     * @param userLogin           the authenticated user's login; never {@code null}
     * @param candidateProjectIds project IDs within the current workspace; never {@code null}
     * @return the subset of {@code candidateProjectIds} the user has access to. Never {@code null}; may be empty.
     */
    List<Long> filterByMembership(String userLogin, List<Long> candidateProjectIds);
}
