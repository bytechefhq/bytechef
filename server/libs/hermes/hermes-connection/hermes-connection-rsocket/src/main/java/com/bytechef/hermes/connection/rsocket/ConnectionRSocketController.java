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

package com.bytechef.hermes.connection.rsocket;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * @author Ivica Cardic
 */
@Controller
public class ConnectionRSocketController {

    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI2")
    public ConnectionRSocketController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @MessageMapping("createConnection")
    public Mono<Connection> createConnection(Connection connection) {
        return Mono.create(sink -> sink.success(connectionService.add(connection)));
    }

    @MessageMapping("getConnection")
    public Mono<Connection> getConnection(String id) {
        return Mono.create(sink -> sink.success(connectionService.getConnection(id)));
    }

    @MessageMapping("getConnections")
    public Mono<List<Connection>> getConnections() {
        return Mono.create(sink -> sink.success(connectionService.getConnections()));
    }

    @MessageMapping("removeConnection")
    public Mono<List<Connection>> removeConnection(String id) {
        connectionService.delete(id);

        return Mono.empty();
    }

    @MessageMapping("updateConnection")
    public Mono<Connection> updateConnection(Connection connection) {
        return Mono.create(sink -> sink.success(connectionService.update(connection.getId(), connection.getName())));
    }
}
