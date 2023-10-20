
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

package com.bytechef.hermes.connection.rsocket.service;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class ConnectionServiceRSocketController {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceRSocketController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @MessageMapping("createConnection")
    public Mono<Connection> createConnection(Connection connection) {
        return Mono
            .create(sink -> sink.success(connectionService.create(connection)));
    }

    @MessageMapping("getConnection")
    public Mono<Connection> getConnection(long id) {
        return Mono.create(sink -> sink.success(connectionService.getConnection(id)));
    }

    @MessageMapping("getConnections")
    @SuppressWarnings("unchecked")
    public Mono<List<Connection>> getConnections(Map<String, Object> map) {
        return Mono.create(sink -> sink.success(connectionService
            .getConnections((List<String>) map.get("componentNames"), (List<Long>) map.get("tagIds"))));
    }

    @MessageMapping("removeConnection")
    public Mono<List<Connection>> removeConnection(long id) {
        connectionService.delete(id);

        return Mono.empty();
    }

    @MessageMapping("updateConnection")
    public Mono<Connection> updateConnection(Connection connection) {

        return Mono.create(sink -> sink.success(
            connectionService.update(
                connection.getId(), connection.getName(), connection.getTagIds(), connection.getVersion())));
    }

    @MessageMapping("updateConnectionTags")
    public Mono<Connection> updateConnectionTags(Map<String, Object> map) {

        return Mono.create(sink -> sink.success(
            connectionService.update(MapValueUtils.getLong(map, "id"),
                MapValueUtils.getList(map, "tagIds", Long.class))));
    }
}
