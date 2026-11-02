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

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads through the plural {@code getConnections}, which returns nothing for an unknown id, rather than
 * {@code getConnection}, which throws: an absent connection has no environment, while any other failure must reach the
 * permission check and deny.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectionEnvironmentResolver(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public String resourceType() {
        return "Connection";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        List<Connection> connections = connectionService.getConnections(List.of(number.longValue()));

        if (connections.isEmpty()) {
            return Optional.empty();
        }

        Connection connection = connections.getFirst();

        int environmentId = connection.getEnvironmentId();

        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            throw new IllegalStateException(
                "Connection " + connection.getId() + " stores unknown environment ordinal " + environmentId);
        }

        return Optional.of(environments[environmentId]);
    }
}
