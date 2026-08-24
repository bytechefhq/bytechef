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

package com.bytechef.automation.search.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class AutomationSearchFacadeImpl implements AutomationSearchFacade {

    private final List<SearchAssetProvider> providers;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AutomationSearchFacadeImpl(
        List<SearchAssetProvider> searchAssetProviders, UserService userService, WorkspaceFacade workspaceFacade) {

        this.providers = List.copyOf(searchAssetProviders);
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult<?>> search(String query, int limit, @Nullable Set<SearchAssetType> types) {
        List<CompletableFuture<List<SearchResult<?>>>> futures = new ArrayList<>();

        String currentTenantId = TenantContext.getCurrentTenantId();

        // Captured here and re-established inside every pooled task, exactly like the tenant id above: the fan-out
        // runs on the common ForkJoinPool, which inherits neither. A provider that resolves resource visibility
        // reads the current principal, and SecurityUtils.getCurrentUserLogin() throws when there is none — so
        // without this the whole search fails rather than degrading.
        SecurityContext securityContext = SecurityContextHolder.getContext();

        // Resolved once here rather than per provider — the answer is the same for every one of them.
        Set<Long> accessibleWorkspaceIds = getAccessibleWorkspaceIds();

        // Filtered before the fan-out, not after: a type-scoped sub-mode (e.g. "Open workflow") must only query the
        // providers it needs, not query every provider and discard the rest.
        List<SearchAssetProvider> requestedProviders = types == null || types.isEmpty()
            ? providers
            : providers.stream()
                .filter(provider -> types.contains(provider.getAssetType()))
                .toList();

        for (SearchAssetProvider provider : requestedProviders) {
            CompletableFuture<List<SearchResult<?>>> future = CompletableFuture.supplyAsync(() -> {
                SecurityContextHolder.setContext(securityContext);

                try {
                    return (List<SearchResult<?>>) TenantContext.callWithTenantId(
                        currentTenantId, () -> provider.search(query, limit));
                } finally {
                    // Pooled threads outlive the request, so the principal must not be left behind on one.
                    SecurityContextHolder.clearContext();
                }
            });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        return futures.stream()
            .flatMap(future -> future.join()
                .stream())
            .filter(searchResult -> isAccessible(searchResult, accessibleWorkspaceIds))
            .toList();
    }

    private Set<Long> getAccessibleWorkspaceIds() {
        return workspaceFacade.getUserWorkspaces(userService.getCurrentUser()
            .getId())
            .stream()
            .map(Workspace::getId)
            .collect(Collectors.toSet());
    }

    private static boolean isAccessible(SearchResult<?> searchResult, Set<Long> accessibleWorkspaceIds) {
        Long workspaceId = searchResult.workspaceId();

        // A null workspaceId means the provider has not been workspace-scoped yet (passed through unchanged during the
        // incremental rollout) or the result is workspace-independent.
        return workspaceId == null || accessibleWorkspaceIds.contains(workspaceId);
    }
}
