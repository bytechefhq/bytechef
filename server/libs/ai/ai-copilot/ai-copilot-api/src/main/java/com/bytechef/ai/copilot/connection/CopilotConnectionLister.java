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

package com.bytechef.ai.copilot.connection;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * SPI for listing the connections the copilot can offer for a component before {@code createConnection}, so the LLM can
 * suggest an existing connection instead of forcing the user through the create dialog. Each surface contributes an
 * implementation that {@link #supports(CopilotConnectionRequest) claims} the requests it can serve: the in-editor / AI
 * Hub surface lists the workspace's connections scoped to the current environment; the embedded workflow chat has no
 * workspace and lists the connected user's connections instead. The {@code listConnectionsForComponent} tool builds a
 * {@link CopilotConnectionRequest} from its invocation context and dispatches to the first supporting implementation.
 *
 * @author Ivica Cardic
 */
public interface CopilotConnectionLister {

    /**
     * Whether this implementation can serve the given request. The tool dispatches to the first implementation that
     * returns {@code true}; implementations must key on mutually exclusive request state (e.g. presence of a workspace
     * vs. an embedded external user) so at most one claims any request.
     *
     * @param request the request describing the current chat surface
     * @return {@code true} if this implementation can list connections for the request
     */
    boolean supports(CopilotConnectionRequest request);

    /**
     * Lists the connections available for the request's component. Only called when {@link #supports} returned
     * {@code true} for the same request.
     *
     * @param request the request describing the component, environment, and calling principal
     * @return the matching connections, possibly empty
     */
    List<CopilotConnection> listConnections(CopilotConnectionRequest request);

    /**
     * Surface-neutral description of a connection-listing request. Carries the union of the fields the surfaces need;
     * each implementation reads only the ones relevant to it. Built by the tool from its invocation context, so the SPI
     * stays free of the tool's context type (and of Spring Security types — the embedded external user id is resolved
     * by the tool from its captured authentication).
     *
     * @param workspaceId       the workspace id when invoked from a workspace surface, otherwise {@code null}
     * @param userId            the platform user id when available (used to rehydrate the workspace security context)
     * @param externalUserId    the embedded connected user's external id when invoked from embedded chat, otherwise
     *                          {@code null}
     * @param environmentId     the {@code Environment} ordinal to scope connections to
     * @param componentName     the component to filter connections by
     * @param connectionVersion the connection definition version to filter by, or {@code null} to not filter by version
     */
    record CopilotConnectionRequest(
        @Nullable Long workspaceId, @Nullable Long userId, @Nullable String externalUserId, int environmentId,
        String componentName, @Nullable Integer connectionVersion) {
    }

    record CopilotConnection(long id, String name, int environmentId, boolean active) {
    }
}
