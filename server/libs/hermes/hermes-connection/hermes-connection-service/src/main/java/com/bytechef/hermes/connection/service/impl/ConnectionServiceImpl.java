
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.connection.service.impl;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(
        @NonNull String name, @NonNull String componentName, int componentVersion, String authorizationName,
        @NonNull Map<String, Object> parameters) {

        Assert.notNull(name, "'name' must not be null.");
        Assert.notNull(componentName, "'componentName' must not be null.");
        Assert.notNull(parameters, "'parameters' must not be null.");

        Connection connection = new Connection();

        connection.setAuthorizationName(authorizationName);
        connection.setComponentName(componentName);
        connection.setComponentVersion(componentVersion);
        connection.setName(name);
        connection.setParameters(parameters);

        return connectionRepository.save(connection);
    }

    @Override
    public void delete(@NonNull Long id) {
        Assert.notNull(id, "'id' must not be null.");

        connectionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(@NonNull Long id) {
        Assert.notNull(id, "'id' must not be null.");

        return connectionRepository.findById(id)
            .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections() {
        return StreamSupport.stream(connectionRepository.findAll()
            .spliterator(), false)
            .toList();
    }

    @Override
    public Connection update(@NonNull Long id, @NonNull String name) {
        Assert.notNull(id, "'id' must not be null.");
        Assert.notNull(name, "'name' must not be null.");

        return connectionRepository
            .findById(id)
            .map(connection -> {
                connection.setName(name);

                return connectionRepository.save(connection);
            })
            .orElseThrow();
    }
}
