
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

package com.bytechef.hermes.connection.rsocket.client.service;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionServiceRSocketClient implements ConnectionService {

    private final RSocketRequester rSocketRequester;

    public ConnectionServiceRSocketClient(RSocketRequester rSocketRequester) {
        this.rSocketRequester = rSocketRequester;
    }

    @Override
    public Connection create(Connection connection) {
        return rSocketRequester
            .route("createConnection")
            .data(connection)
            .retrieveMono(Connection.class)
            .block();
    }

    @Override
    public void delete(long id) {
        rSocketRequester.route("removeConnection")
            .data(id)
            .send()
            .block();
    }

    @Override
    public Connection getConnection(long id) {
        return rSocketRequester
            .route("getConnection")
            .data(id)
            .retrieveMono(Connection.class)
            .block();
    }

    @Override
    public List<Connection> getConnections() {
        return getConnections(null, null);
    }

    @Override
    public List<Connection> getConnections(List<String> componentNames, List<Long> tagIds) {
        return rSocketRequester
            .route("getConnections")
            .data(new HashMap<>() {
                {
                    put("componentNames", componentNames);
                    put("tagIds", tagIds);
                }
            })
            .retrieveMono(new ParameterizedTypeReference<List<Connection>>() {})
            .block();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        return rSocketRequester
            .route("updateConnectionTags")
            .data(Map.of("id", id, "tagIds", tagIds))
            .retrieveMono(Connection.class)
            .block();
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        return rSocketRequester
            .route("updateConnection")
            .data(new Connection(id, name, tagIds, version))
            .retrieveMono(Connection.class)
            .block();
    }
}
