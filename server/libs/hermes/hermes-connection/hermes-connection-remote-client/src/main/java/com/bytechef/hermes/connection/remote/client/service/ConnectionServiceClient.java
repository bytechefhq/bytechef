
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

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionServiceClient implements ConnectionService {

    @Override
    public Connection create(Connection connection) {
        return null;
    }

    @Override
    public void delete(long id) {

    }

    @Override
    public Connection getConnection(long id) {
        return null;
    }

    @Override
    public List<Connection> getConnections() {
        return null;
    }

    @Override
    public List<Connection> getConnections(List<Long> ids) {
        return null;
    }

    @Override
    public List<Connection> getConnections(String componentName, int version) {
        return null;
    }

    @Override
    public List<Connection> getConnections(List<String> componentNames, List<Long> tagIds) {
        return null;
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        return null;
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        return null;
    }
}
