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

package com.bytechef.automation.workspacefile.web.graphql;

import com.bytechef.automation.workspacefile.config.AutomationWorkspaceFileQuotaProperties;
import com.bytechef.automation.workspacefile.domain.WorkspaceFile;
import com.bytechef.automation.workspacefile.service.WorkspaceFileFacade;
import com.bytechef.automation.workspacefile.service.WorkspaceFileTagService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing queries and mutations for workspace files.
 *
 * @author Ivica Cardic
 */
@Controller
@SuppressFBWarnings("EI")
public class WorkspaceFileGraphQlController {

    private final AutomationWorkspaceFileQuotaProperties quotaProperties;
    private final TagService tagService;
    private final WorkspaceFileFacade workspaceFileFacade;
    private final WorkspaceFileTagService workspaceFileTagService;

    @SuppressFBWarnings("EI")
    public WorkspaceFileGraphQlController(
        AutomationWorkspaceFileQuotaProperties quotaProperties, TagService tagService,
        WorkspaceFileFacade workspaceFileFacade, WorkspaceFileTagService workspaceFileTagService) {

        this.quotaProperties = quotaProperties;
        this.tagService = tagService;
        this.workspaceFileFacade = workspaceFileFacade;
        this.workspaceFileTagService = workspaceFileTagService;
    }

    @QueryMapping
    public WorkspaceFile workspaceFile(@Argument Long id) {
        return workspaceFileFacade.findById(id);
    }

    @QueryMapping
    public List<WorkspaceFile> workspaceFiles(
        @Argument Long workspaceId, @Argument List<Long> tagIds, @Argument String mimeTypePrefix) {

        List<WorkspaceFile> workspaceFiles = workspaceFileFacade.findAllByWorkspaceId(workspaceId, tagIds);

        if (mimeTypePrefix == null) {
            return workspaceFiles;
        }

        return workspaceFiles.stream()
            .filter(workspaceFile -> workspaceFile.getMimeType() != null
                && workspaceFile.getMimeType()
                    .startsWith(mimeTypePrefix))
            .toList();
    }

    @QueryMapping
    public List<Tag> workspaceFileTags(@Argument Long workspaceId) {
        return workspaceFileTagService.getAllTags();
    }

    @QueryMapping
    public String workspaceFileTextContent(@Argument Long id) {
        WorkspaceFile workspaceFile = workspaceFileFacade.findById(id);

        if (workspaceFile.getSizeBytes() > quotaProperties.maxTextEditBytes()) {
            throw new IllegalArgumentException("File too large for text editing; use download instead.");
        }

        try (InputStream inputStream = workspaceFileFacade.downloadContent(id)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @MutationMapping
    public WorkspaceFile updateWorkspaceFile(@Argument UpdateWorkspaceFileInput input) {
        if (input.name() != null) {
            workspaceFileFacade.rename(input.id(), input.name());
        }

        return workspaceFileFacade.findById(input.id());
    }

    @MutationMapping
    public WorkspaceFile updateWorkspaceFileTextContent(@Argument Long id, @Argument String content) {
        return workspaceFileFacade.updateContent(
            id, "text/plain", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @MutationMapping
    public boolean deleteWorkspaceFile(@Argument Long id) {
        workspaceFileFacade.delete(id);

        return true;
    }

    @MutationMapping
    public WorkspaceFile updateWorkspaceFileTags(@Argument Long id, @Argument List<Long> tagIds) {
        return workspaceFileFacade.updateTags(id, tagIds);
    }

    @SchemaMapping(typeName = "WorkspaceFile", field = "downloadUrl")
    public String downloadUrl(WorkspaceFile workspaceFile) {
        return "/api/automation/internal/workspace-files/%d/content".formatted(workspaceFile.getId());
    }

    @SchemaMapping(typeName = "WorkspaceFile", field = "source")
    public String source(WorkspaceFile workspaceFile) {
        return workspaceFile.getSource()
            .name();
    }

    @SchemaMapping(typeName = "WorkspaceFile", field = "tags")
    public List<Tag> tags(WorkspaceFile workspaceFile) {
        List<Long> tagIds = workspaceFile.getTagIds();

        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @SchemaMapping(typeName = "WorkspaceFile", field = "createdDate")
    public Long createdDate(WorkspaceFile workspaceFile) {
        Instant createdDate = workspaceFile.getCreatedDate();

        return createdDate == null ? null : createdDate.toEpochMilli();
    }

    @SchemaMapping(typeName = "WorkspaceFile", field = "lastModifiedDate")
    public Long lastModifiedDate(WorkspaceFile workspaceFile) {
        Instant lastModifiedDate = workspaceFile.getLastModifiedDate();

        return lastModifiedDate == null ? null : lastModifiedDate.toEpochMilli();
    }

    public record UpdateWorkspaceFileInput(Long id, String name, String description) {
    }
}
