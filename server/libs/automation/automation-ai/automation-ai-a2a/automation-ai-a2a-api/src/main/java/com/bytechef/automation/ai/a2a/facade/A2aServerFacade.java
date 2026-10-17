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
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface A2aServerFacade {

    /**
     * Creates an A2A server in a workspace.
     *
     * @param workspaceId the ID of the workspace the server belongs to
     * @param a2aServer   the server to create
     * @return the created server
     */
    A2aServer createA2aServer(long workspaceId, A2aServer a2aServer);

    /**
     * Deletes an A2A server.
     *
     * @param id the ID of the A2A server
     */
    void deleteA2aServer(long id);

    /**
     * Returns the tags assigned to any A2A server of a workspace.
     *
     * @param workspaceId the ID of the workspace
     * @return the tags used by the workspace's A2A servers
     */
    List<Tag> getA2aServerTags(long workspaceId);

    /**
     * Returns the tags of each of the given A2A servers.
     *
     * @param a2aServers the A2A servers to return the tags of
     * @return the tags of each A2A server
     */
    Map<A2aServer, List<Tag>> getA2aServerTags(List<A2aServer> a2aServers);

    /**
     * Replaces the tags of an A2A server, creating the tags that do not exist yet.
     *
     * @param id   the ID of the A2A server
     * @param tags the tags to assign, new ones without an ID
     * @return the assigned tags
     */
    List<Tag> updateA2aServerTags(long id, List<Tag> tags);

    /**
     * Returns the A2A servers of a workspace.
     *
     * @param workspaceId the ID of the workspace
     * @return the workspace's A2A servers
     */
    List<A2aServer> getWorkspaceA2aServers(long workspaceId);

    /**
     * Updates an A2A server, leaving each property passed as {@code null} unchanged.
     *
     * @param id                     the ID of the A2A server
     * @param name                   the new name
     * @param description            the new description
     * @param enabled                whether the server answers requests
     * @param authenticationRequired whether the server requires an API key
     * @return the updated server
     */
    A2aServer updateA2aServer(
        long id, @Nullable String name, @Nullable String description, @Nullable Boolean enabled,
        @Nullable Boolean authenticationRequired);
}
