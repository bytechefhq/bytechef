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

package com.bytechef.automation.configuration.search;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ConnectionSearchAssetProvider implements SearchAssetProvider {

    private final ConnectionService connectionService;
    private final WorkspaceConnectionService workspaceConnectionService;

    ConnectionSearchAssetProvider(
        ConnectionService connectionService, WorkspaceConnectionService workspaceConnectionService) {

        this.connectionService = connectionService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public List<ConnectionSearchResult> search(String query, int limit, Set<Long> workspaceIds) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        Set<Long> connectionIds = workspaceIds.stream()
            .flatMap(workspaceId -> workspaceConnectionService.getWorkspaceConnections(workspaceId)
                .stream())
            .map(WorkspaceConnection::getConnectionId)
            .collect(Collectors.toSet());

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        return connectionService.getConnections(PlatformType.AUTOMATION)
            .stream()
            .filter(connection -> connectionIds.contains(connection.getId()))
            .filter(connection -> containsIgnoreCase(connection.getName(), queryLower))
            .limit(limit)
            .map(connection -> new ConnectionSearchResult(connection.getId(), connection.getName()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.CONNECTION;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
