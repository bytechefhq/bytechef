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

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface A2aServerFacade {

    /**
     * Returns the tags assigned to any A2A server.
     */
    List<Tag> getA2aServerTags();

    /**
     * Returns the tags of each of the given A2A servers.
     */
    Map<A2aServer, List<Tag>> getA2aServerTags(List<A2aServer> a2aServers);

    /**
     * Replaces the tags of an A2A server, creating tags that do not exist yet.
     */
    List<Tag> updateA2aServerTags(long id, List<Tag> tags);
}
