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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionVisibilityProvider implements ResourceVisibilityProvider {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionVisibilityProvider(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return connectionService.fetchConnection(id)
            .map(ConnectionVisibilityProvider::toVisibilityRecord);
    }

    private static VisibilityRecord toVisibilityRecord(Connection connection) {
        return new VisibilityRecord(connection.getId(), connection.getVisibility(), connection.getCreatedBy());
    }
}
