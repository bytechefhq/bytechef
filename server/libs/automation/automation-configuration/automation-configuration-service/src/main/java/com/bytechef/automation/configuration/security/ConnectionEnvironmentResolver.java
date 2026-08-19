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
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
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

        // The connection stores the environment as its ordinal, and a missing connection resolves to empty rather
        // than throwing, so a deleted row falls back to the environment-unaware check instead of erroring.
        try {
            Connection connection = connectionService.getConnection(number.longValue());

            return Optional.of(Environment.values()[connection.getEnvironmentId()]);
        } catch (RuntimeException runtimeException) {
            return Optional.empty();
        }
    }
}
