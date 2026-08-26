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

package com.bytechef.automation.ai.a2a.service;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing {@link A2aServer} entities.
 *
 * @author Ivica Cardic
 */
public interface A2aServerService {

    A2aServer create(A2aServer a2aServer);

    A2aServer create(String name, String description, PlatformType type, Environment environment);

    void delete(long a2aServerId);

    Optional<A2aServer> fetchA2aServer(long a2aServerId);

    /**
     * Retrieves an A2A server by its cross-environment lineage identifier and environment.
     *
     * @param uuid        the cross-environment lineage identifier shared by the server's counterparts in other
     *                    environments
     * @param environment the environment to look the server up in
     * @return the A2A server matching the given uuid and environment, if one exists
     */
    Optional<A2aServer> fetchA2aServer(UUID uuid, Environment environment);

    A2aServer getA2aServer(long a2aServerId);

    /**
     * Resolves the A2A server addressed by the given secret key (the path/tenant anchor of an A2A endpoint).
     *
     * @param secretKey the server's secret key
     * @return the matching A2A server
     * @throws IllegalArgumentException if no server has the given secret key
     */
    A2aServer getA2aServer(String secretKey);

    List<A2aServer> getA2aServers(PlatformType type);

    A2aServer update(A2aServer a2aServer);
}
