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
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aServerFacadeImpl implements A2aServerFacade {

    private final A2aServerService a2aServerService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public A2aServerFacadeImpl(A2aServerService a2aServerService, TagService tagService) {
        this.a2aServerService = a2aServerService;
        this.tagService = tagService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'A2A_VIEW')")
    public List<Tag> getA2aServerTags(long workspaceId) {
        List<Long> tagIds = a2aServerService.getWorkspaceA2aServers(workspaceId)
            .stream()
            .map(A2aServer::getTagIds)
            .flatMap(Collection::stream)
            .distinct()
            .toList();

        return tagIds.isEmpty() ? List.of() : tagService.getTags(tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<A2aServer, List<Tag>> getA2aServerTags(List<A2aServer> a2aServers) {
        List<Long> tagIds = a2aServers.stream()
            .map(A2aServer::getTagIds)
            .flatMap(Collection::stream)
            .distinct()
            .toList();

        Map<Long, Tag> tagMap = tagIds.isEmpty() ? Map.of()
            : tagService.getTags(tagIds)
                .stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));

        return a2aServers.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    a2aServer -> a2aServer.getTagIds()
                        .stream()
                        .map(tagMap::get)
                        .filter(Objects::nonNull)
                        .toList()));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'A2aServer', 'A2A_EDIT')")
    public List<Tag> updateA2aServerTags(long id, List<Tag> tags) {
        List<Tag> savedTags = tags.isEmpty() ? List.of() : tagService.save(tags);

        a2aServerService.updateTags(
            id, savedTags.stream()
                .map(Tag::getId)
                .toList());

        return savedTags;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'A2A_VIEW')")
    public List<A2aServer> getWorkspaceA2aServers(long workspaceId) {
        return a2aServerService.getWorkspaceA2aServers(workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'A2A_CREATE')")
    public A2aServer createA2aServer(long workspaceId, A2aServer a2aServer) {
        a2aServer.setWorkspaceId(workspaceId);

        return a2aServerService.create(a2aServer);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'A2aServer', 'A2A_EDIT')")
    public void deleteA2aServer(long id) {
        a2aServerService.delete(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'A2aServer', 'A2A_EDIT')")
    public A2aServer updateA2aServer(
        long id, @Nullable String name, @Nullable String description, @Nullable Boolean enabled,
        @Nullable Boolean authenticationRequired) {

        A2aServer a2aServer = a2aServerService.getA2aServer(id);

        if (name != null) {
            a2aServer.setName(name);
        }

        if (description != null) {
            a2aServer.setDescription(description);
        }

        if (enabled != null) {
            a2aServer.setEnabled(enabled);
        }

        if (authenticationRequired != null) {
            a2aServer.setAuthenticationRequired(authenticationRequired);
        }

        return a2aServerService.update(a2aServer);
    }
}
