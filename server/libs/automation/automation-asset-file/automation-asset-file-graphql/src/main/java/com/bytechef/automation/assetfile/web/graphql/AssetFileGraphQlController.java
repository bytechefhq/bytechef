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

package com.bytechef.automation.assetfile.web.graphql;

import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileVersion;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.service.AssetFileTagService;
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
public class AssetFileGraphQlController {

    private final AssetFileGraphQlAccessGuard accessGuard;
    private final AutomationAssetFileQuotaProperties quotaProperties;
    private final AutomationAssetFileSharingProperties sharingProperties;
    private final TagService tagService;
    private final AssetFileFacade assetFileFacade;
    private final AssetFileTagService assetFileTagService;

    @SuppressFBWarnings("EI")
    public AssetFileGraphQlController(
        AssetFileGraphQlAccessGuard accessGuard, AutomationAssetFileQuotaProperties quotaProperties,
        AutomationAssetFileSharingProperties sharingProperties, TagService tagService,
        AssetFileFacade assetFileFacade, AssetFileTagService assetFileTagService) {

        this.accessGuard = accessGuard;
        this.quotaProperties = quotaProperties;
        this.sharingProperties = sharingProperties;
        this.tagService = tagService;
        this.assetFileFacade = assetFileFacade;
        this.assetFileTagService = assetFileTagService;
    }

    @QueryMapping
    public AssetFile assetFile(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        return assetFileFacade.findById(id);
    }

    @QueryMapping
    public List<AssetFile> assetFiles(
        @Argument Long workspaceId, @Argument Integer environment, @Argument List<Long> tagIds,
        @Argument String mimeTypePrefix) {

        accessGuard.verifyWorkspaceAccess(workspaceId);

        // Default to DEVELOPMENT (ordinal 0) when the client did not pass environment so legacy callers do not
        // suddenly receive an empty list. Once every consumer is updated this default can become a hard error.
        int environmentOrdinal = environment == null ? 0 : environment;

        List<AssetFile> assetFiles = assetFileFacade.findAllByWorkspaceIdAndEnvironment(
            workspaceId, environmentOrdinal, tagIds);

        if (mimeTypePrefix == null) {
            return assetFiles;
        }

        return assetFiles.stream()
            .filter(assetFile -> assetFile.getMimeType() != null
                && assetFile.getMimeType()
                    .startsWith(mimeTypePrefix))
            .toList();
    }

    @QueryMapping
    public List<Tag> assetFileTags(@Argument Long workspaceId) {
        return assetFileTagService.getAllTags(workspaceId);
    }

    @QueryMapping
    public String assetFileTextContent(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        AssetFile assetFile = assetFileFacade.findById(id);

        if (assetFile.getSizeBytes() > quotaProperties.maxTextEditBytes()) {
            throw new IllegalArgumentException("File too large for text editing; use download instead.");
        }

        try (InputStream inputStream = assetFileFacade.downloadContent(id)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @MutationMapping
    public AssetFile updateAssetFile(@Argument UpdateAssetFileInput input) {
        accessGuard.verifyFileAccess(input.id());

        if (input.name() != null) {
            assetFileFacade.rename(input.id(), input.name());
        }

        // A null description means "leave unchanged" (the client omits the field on rename-only updates); clearing a
        // description is expressed as an empty string.
        if (input.description() != null) {
            assetFileFacade.updateDescription(input.id(), input.description());
        }

        return assetFileFacade.findById(input.id());
    }

    @MutationMapping
    public AssetFile updateAssetFileTextContent(@Argument Long id, @Argument String content) {
        accessGuard.verifyFileAccess(id);

        return assetFileFacade.updateContent(
            id, "text/plain", new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    @MutationMapping
    public boolean deleteAssetFile(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        assetFileFacade.delete(id);

        return true;
    }

    @QueryMapping
    public List<AssetFileVersion> assetFileVersions(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        return assetFileFacade.getVersions(id);
    }

    @QueryMapping
    public String assetFileSignedDownloadUrl(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        return "/api/automation/asset-files/signed/%s".formatted(assetFileFacade.createSignedDownloadToken(id));
    }

    @MutationMapping
    public AssetFile restoreAssetFileVersion(@Argument Long id, @Argument Long versionId) {
        accessGuard.verifyFileAccess(id);

        return assetFileFacade.restoreVersion(id, versionId);
    }

    @MutationMapping
    public AssetFile enableAssetFilePublicLink(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        assetFileFacade.enablePublicLink(id);

        return assetFileFacade.findById(id);
    }

    @MutationMapping
    public AssetFile disableAssetFilePublicLink(@Argument Long id) {
        accessGuard.verifyFileAccess(id);

        assetFileFacade.disablePublicLink(id);

        return assetFileFacade.findById(id);
    }

    @MutationMapping
    public AssetFile updateAssetFileTags(@Argument UpdateAssetFileTagsInput input) {
        accessGuard.verifyFileAccess(input.id());

        List<Tag> tags = input.tags() == null
            ? List.of()
            : input.tags()
                .stream()
                .map(tagInput -> {
                    Tag tag = new Tag();

                    if (tagInput.id() != null) {
                        tag.setId(tagInput.id());
                    }

                    tag.setName(tagInput.name());

                    return tag;
                })
                .toList();

        assetFileTagService.updateTags(input.id(), tags);

        return assetFileFacade.findById(input.id());
    }

    @SchemaMapping(typeName = "AssetFile", field = "downloadUrl")
    public String downloadUrl(AssetFile assetFile) {
        return "/api/automation/internal/asset-files/%d/content".formatted(assetFile.getId());
    }

    @SchemaMapping(typeName = "AssetFile", field = "publicLinkUrl")
    public String publicLinkUrl(AssetFile assetFile) {
        // Hidden while the operator kill-switch is off: the link would 404 anyway, and showing it would suggest the
        // file is still shared externally when it is not.
        if (!sharingProperties.publicLinkEnabled() || assetFile.getPublicLinkToken() == null) {
            return null;
        }

        return "/api/automation/asset-files/public/%s".formatted(assetFile.getPublicLinkToken());
    }

    @SchemaMapping(typeName = "AssetFileVersion", field = "createdDate")
    public Long assetFileVersionCreatedDate(AssetFileVersion assetFileVersion) {
        Instant createdDate = assetFileVersion.getCreatedDate();

        return createdDate == null ? null : createdDate.toEpochMilli();
    }

    @SchemaMapping(typeName = "AssetFile", field = "source")
    public String source(AssetFile assetFile) {
        return assetFile.getSource()
            .name();
    }

    @SchemaMapping(typeName = "AssetFile", field = "format")
    public String format(AssetFile assetFile) {
        return assetFile.getFormat() == null ? null : assetFile.getFormat()
            .name();
    }

    @SchemaMapping(typeName = "AssetFile", field = "tags")
    public List<Tag> tags(AssetFile assetFile) {
        List<Long> tagIds = assetFile.getTagIds();

        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        return tagService.getTags(tagIds);
    }

    @SchemaMapping(typeName = "AssetFile", field = "createdDate")
    public Long createdDate(AssetFile assetFile) {
        Instant createdDate = assetFile.getCreatedDate();

        return createdDate == null ? null : createdDate.toEpochMilli();
    }

    @SchemaMapping(typeName = "AssetFile", field = "lastModifiedDate")
    public Long lastModifiedDate(AssetFile assetFile) {
        Instant lastModifiedDate = assetFile.getLastModifiedDate();

        return lastModifiedDate == null ? null : lastModifiedDate.toEpochMilli();
    }

    public record UpdateAssetFileInput(Long id, String name, String description) {
    }

    public record UpdateAssetFileTagsInput(Long id, List<TagInput> tags) {
    }

    public record TagInput(Long id, String name) {
    }
}
