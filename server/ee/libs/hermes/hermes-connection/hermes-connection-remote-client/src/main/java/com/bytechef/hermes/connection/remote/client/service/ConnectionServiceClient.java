
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

package com.bytechef.hermes.connection.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionServiceClient implements ConnectionService {

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public ConnectionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Connection create(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Connection> fetchConnection(long id) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host("connection-service-app")
                    .path("/api/internal/connection-service/fetch-connection/{id}")
                    .build(id),
                Connection.class));
    }

    @Override
    public Connection getConnection(long id) {
        return fetchConnection(id).orElseThrow();
    }

    @Override
    public List<Connection> getConnections() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("connection-service-app")
                .path("/api/internal/connection-service/get-connections")
                .build(),
            new ParameterizedTypeReference<List<Connection>>() {});
    }

    @Override
    public List<Connection> getConnections(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, Integer connectionVersion, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(Connection connection) {
        throw new UnsupportedOperationException();
    }
}
