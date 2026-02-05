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

import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class AutomationSearchFacadeImpl implements AutomationSearchFacade {

    private final List<SearchAssetProvider> providers;

    public AutomationSearchFacadeImpl(List<SearchAssetProvider> searchAssetProviders) {
        this.providers = List.copyOf(searchAssetProviders);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SearchResult<?>> search(String query, int limit) {
        List<CompletableFuture<List<SearchResult<?>>>> futures = new ArrayList<>();

        String currentTenantId = TenantContext.getCurrentTenantId();

        for (SearchAssetProvider provider : providers) {
            CompletableFuture<List<SearchResult<?>>> future = CompletableFuture.supplyAsync(
                () -> (List<SearchResult<?>>) TenantContext.callWithTenantId(currentTenantId,
                    () -> provider.search(query, limit)));

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        return futures.stream()
            .flatMap(future -> future.join()
                .stream())
            .toList();
    }
}
