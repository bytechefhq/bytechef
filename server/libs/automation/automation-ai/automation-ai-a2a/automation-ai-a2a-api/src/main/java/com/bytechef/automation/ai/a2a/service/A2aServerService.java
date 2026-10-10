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
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface A2aServerService {

    A2aServer create(A2aServer a2aServer);

    void delete(long a2aServerId);

    Optional<A2aServer> fetchA2aServer(String secretKey);

    A2aServer getA2aServer(long a2aServerId);

    A2aServer getA2aServer(String secretKey);

    String getA2aServerSecretKey(long a2aServerId);

    List<A2aServer> getA2aServers();

    A2aServer update(A2aServer a2aServer);

    A2aServer updateTags(long id, List<Long> tagIds);
}
