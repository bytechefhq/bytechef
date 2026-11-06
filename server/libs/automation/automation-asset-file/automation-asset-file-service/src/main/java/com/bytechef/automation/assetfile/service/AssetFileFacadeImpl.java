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

package com.bytechef.automation.assetfile.service;

import com.bytechef.automation.assetfile.config.AutomationAssetFileQuotaProperties;
import com.bytechef.automation.assetfile.config.AutomationAssetFileSharingProperties;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.automation.assetfile.domain.AssetFileSource;
import com.bytechef.automation.assetfile.domain.AssetFileVersion;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.exception.AssetFileQuotaExceededException;
import com.bytechef.automation.assetfile.file.storage.AssetFileFileStorage;
import com.bytechef.automation.assetfile.metric.AssetFileMetrics;
import com.bytechef.automation.assetfile.repository.AssetFileVersionRepository;
import com.bytechef.automation.assetfile.util.AssetFileNameSanitizer;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings("EI2")
public class AssetFileFacadeImpl implements AssetFileFacade {

    private static final Logger log = LoggerFactory.getLogger(AssetFileFacadeImpl.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AssetFileService service;
    private final AssetFileFileStorage fileStorage;
    private final AssetFileMetrics metrics;
    private final AssetFileVersionRepository versionRepository;
    private final AssetFileSystemFacade assetFileSystemFacade;
    private final AssetFileWriteSupport writeSupport;
    private final AutomationAssetFileQuotaProperties quota;
    private final AutomationAssetFileSharingProperties sharingProperties;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AssetFileFacadeImpl(
        AssetFileService service,
        AssetFileFileStorage fileStorage,
        AssetFileMetrics metrics,
        AssetFileVersionRepository versionRepository,
        AssetFileSystemFacade assetFileSystemFacade,
        AssetFileWriteSupport writeSupport,
        AutomationAssetFileQuotaProperties quota,
        AutomationAssetFileSharingProperties sharingProperties,
        UserService userService,
        WorkspaceFacade workspaceFacade) {

        this.service = service;
        this.fileStorage = fileStorage;
        this.metrics = metrics;
        this.versionRepository = versionRepository;
        this.assetFileSystemFacade = assetFileSystemFacade;
        this.writeSupport = writeSupport;
        this.quota = quota;
        this.sharingProperties = sharingProperties;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public AssetFile createFromUpload(
        Long workspaceId, int environment, String filename, String contentType, InputStream data) {

        checkMembership(workspaceId);

        return assetFileSystemFacade.createFromUpload(workspaceId, environment, filename, contentType, data);
    }

    @Override
    public AssetFile createFromAi(
        Long workspaceId, int environment, String filename, String contentType, String content,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        checkMembership(workspaceId);

        String sanitized = writeSupport.resolveUniqueName(
            workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        enforceSingleFileQuota(bytes.length);
        writeSupport.enforceWorkspaceQuota(workspaceId, environment, bytes.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        AssetFile assetFile = new AssetFile();

        assetFile.setName(sanitized);
        assetFile.setMimeType(contentType);
        assetFile.setSizeBytes(bytes.length);
        assetFile.setFile(stored);
        assetFile.setSource(AssetFileSource.AI_GENERATED);
        assetFile.setFormat(format);
        assetFile.setMetadataJson(metadataJson);
        assetFile.setGeneratedByAgentSource(generatedByAgentSource);
        assetFile.setGeneratedFromPrompt(generatedFromPrompt);
        assetFile.setEnvironment(Environment.values()[environment]);

        AssetFile saved;

        try {
            saved = service.create(assetFile, workspaceId);
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.AI_GENERATED, contentType);

        return saved;
    }

    @Override
    public AssetFile createBinaryFromAi(
        Long workspaceId, int environment, String filename, String contentType, byte[] data,
        AssetFileFormat format, String metadataJson,
        Short generatedByAgentSource, String generatedFromPrompt) {

        checkMembership(workspaceId);

        String sanitized = writeSupport.resolveUniqueName(
            workspaceId, environment, AssetFileNameSanitizer.sanitize(filename));

        enforceSingleFileQuota(data.length);
        writeSupport.enforceWorkspaceQuota(workspaceId, environment, data.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(data));

        AssetFile assetFile = new AssetFile();

        assetFile.setName(sanitized);
        assetFile.setMimeType(contentType);
        assetFile.setSizeBytes(data.length);
        assetFile.setFile(stored);
        assetFile.setSource(AssetFileSource.AI_GENERATED);
        assetFile.setFormat(format);
        assetFile.setMetadataJson(metadataJson);
        assetFile.setGeneratedByAgentSource(generatedByAgentSource);
        assetFile.setGeneratedFromPrompt(generatedFromPrompt);
        assetFile.setEnvironment(Environment.values()[environment]);

        AssetFile saved;

        try {
            saved = service.create(assetFile, workspaceId);
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.AI_GENERATED, contentType);

        return saved;
    }

    @Override
    public void delete(Long id) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);
        Long workspaceId = writeSupport.resolveWorkspaceIdForFile(assetFile);

        assetFileSystemFacade.deleteInWorkspace(id, workspaceId);
    }

    @Override
    public long getMaxFileSizeBytes() {
        return quota.maxFileSizeBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadContent(Long id) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);
        Long workspaceId = writeSupport.resolveWorkspaceIdForFile(assetFile);

        return assetFileSystemFacade.downloadContentInWorkspace(id, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetFile> findAllByWorkspaceIdAndEnvironment(
        Long workspaceId, int environment, List<Long> tagIds) {

        checkMembership(workspaceId);

        return assetFileSystemFacade.findAllByWorkspaceIdAndEnvironment(workspaceId, environment, tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetFile findById(Long id) {
        checkMembershipOfOwner(id);

        return service.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AssetFile findByIdInWorkspace(Long id, Long workspaceId) {
        checkMembership(workspaceId);

        return assetFileSystemFacade.findByIdInWorkspace(id, workspaceId);
    }

    @Override
    public AssetFile rename(Long id, String newName) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);
        Long workspaceId = writeSupport.resolveWorkspaceIdForFile(assetFile);

        return assetFileSystemFacade.renameInWorkspace(id, workspaceId, newName);
    }

    @Override
    public AssetFile updateDescription(Long id, String description) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);

        assetFile.setDescription(description);

        return service.update(assetFile);
    }

    @Override
    public AssetFile cloneToEnvironment(
        Long id, Long workspaceId, int targetEnvironmentId, String newName) {

        checkMembership(workspaceId);

        Environment[] environments = Environment.values();

        if (targetEnvironmentId < 0 || targetEnvironmentId >= environments.length) {
            throw new IllegalArgumentException("Invalid targetEnvironmentId: " + targetEnvironmentId);
        }

        AssetFile source = assetFileSystemFacade.findByIdInWorkspace(id, workspaceId);

        String requestedName = newName != null && !newName.isBlank() ? newName : source.getName();
        String sanitized = writeSupport.resolveUniqueName(
            workspaceId, targetEnvironmentId, AssetFileNameSanitizer.sanitize(requestedName));

        byte[] bytes = readAllFromStorage(source.getFile());

        enforceSingleFileQuota(bytes.length);
        writeSupport.enforceWorkspaceQuota(workspaceId, targetEnvironmentId, bytes.length);

        FileEntry stored = fileStorage.storeFile(sanitized, new ByteArrayInputStream(bytes));

        AssetFile clone = new AssetFile();

        clone.setName(sanitized);
        clone.setMimeType(source.getMimeType());
        clone.setSizeBytes(bytes.length);
        clone.setFile(stored);
        clone.setSource(AssetFileSource.USER_UPLOAD);
        clone.setEnvironment(environments[targetEnvironmentId]);
        clone.setDescription(source.getDescription());

        AssetFile saved;

        try {
            saved = service.create(clone, workspaceId);
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        metrics.recordCreate(AssetFileSource.USER_UPLOAD, source.getMimeType());

        return saved;
    }

    @Override
    public AssetFile updateContent(Long id, String contentType, InputStream data) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);
        Long workspaceId = writeSupport.resolveWorkspaceIdForFile(assetFile);

        return assetFileSystemFacade.updateContentInWorkspace(id, workspaceId, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetFileVersion> getVersions(Long id) {
        checkMembershipOfOwner(id);

        return versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(id);
    }

    @Override
    public AssetFile restoreVersion(Long id, Long versionId) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);

        AssetFileVersion version = versionRepository.findById(versionId)
            .filter(candidate -> Objects.equals(candidate.getAssetFileId(), id))
            .orElseThrow(() -> new AssetFileNotFoundException(
                "Asset file version %d not found for asset file %d".formatted(versionId, id)));

        byte[] bytes = readAllFromStorage(version.getFile());

        enforceSingleFileQuota(bytes.length);

        return updateContentInternal(assetFile, version.getMimeType(), bytes);
    }

    private AssetFile updateContentInternal(AssetFile assetFile, String mimeType, byte[] bytes) {
        Long workspaceId = writeSupport.resolveWorkspaceIdForFile(assetFile);
        int environment = (int) assetFile.getEnvironmentId();

        long delta = bytes.length - assetFile.getSizeBytes();

        if (delta > 0) {
            writeSupport.enforceWorkspaceQuota(workspaceId, environment, delta);
        }

        FileEntry previousFile = assetFile.getFile();
        String previousMimeType = assetFile.getMimeType();
        long previousSizeBytes = assetFile.getSizeBytes();

        FileEntry stored = fileStorage.storeFile(assetFile.getName(), new ByteArrayInputStream(bytes));

        assetFile.setFile(stored);
        assetFile.setMimeType(mimeType);
        assetFile.setSizeBytes(bytes.length);

        AssetFile saved;

        try {
            saved = service.update(assetFile);

            if (previousFile != null) {
                snapshotVersion(saved.getId(), previousFile, previousMimeType, previousSizeBytes);
                pruneVersions(saved.getId());
            }
        } catch (RuntimeException exception) {
            writeSupport.safeDeleteAfterRollback(stored, exception);

            throw exception;
        }

        return saved;
    }

    private void snapshotVersion(Long assetFileId, FileEntry file, String mimeType, long sizeBytes) {
        int nextVersionNumber = versionRepository.findFirstByAssetFileIdOrderByVersionNumberDesc(assetFileId)
            .map(latest -> latest.getVersionNumber() + 1)
            .orElse(1);

        AssetFileVersion version = new AssetFileVersion();

        version.setAssetFileId(assetFileId);
        version.setVersionNumber(nextVersionNumber);
        version.setFile(file);
        version.setMimeType(mimeType);
        version.setSizeBytes(sizeBytes);

        versionRepository.save(version);
    }

    private void pruneVersions(Long assetFileId) {
        int maxVersions = quota.maxVersionsPerFile();

        if (maxVersions < 0) {
            return;
        }

        List<AssetFileVersion> versions = versionRepository.findAllByAssetFileIdOrderByVersionNumberDesc(assetFileId);

        if (versions.size() <= maxVersions) {
            return;
        }

        for (AssetFileVersion excess : versions.subList(maxVersions, versions.size())) {
            versionRepository.deleteById(excess.getId());

            writeSupport.scheduleBlobDeleteAfterCommit(assetFileId, excess.getFile());
        }
    }

    @Override
    public String enablePublicLink(Long id) {
        if (!sharingProperties.publicLinkEnabled()) {
            throw new IllegalStateException("Public link sharing is disabled by the operator");
        }

        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);

        String existingToken = assetFile.getPublicLinkToken();

        if (existingToken != null) {
            return existingToken;
        }

        byte[] tokenBytes = new byte[32];

        SECURE_RANDOM.nextBytes(tokenBytes);

        Base64.Encoder encoder = Base64.getUrlEncoder()
            .withoutPadding();

        String token = encoder.encodeToString(tokenBytes);

        assetFile.setPublicLinkToken(token);

        service.update(assetFile);

        return token;
    }

    @Override
    public void disablePublicLink(Long id) {
        checkMembershipOfOwner(id);

        AssetFile assetFile = service.findById(id);

        if (assetFile.getPublicLinkToken() == null) {
            return;
        }

        assetFile.setPublicLinkToken(null);

        service.update(assetFile);
    }

    private void enforceSingleFileQuota(long bytes) {
        long limit = quota.maxFileSizeBytes();

        if (limit >= 0 && bytes > limit) {
            throw new AssetFileQuotaExceededException(
                "File size %d exceeds per-file limit %d".formatted(bytes, limit), bytes, limit);
        }
    }

    private byte[] readAllFromStorage(FileEntry fileEntry) {
        try (InputStream inputStream = fileStorage.getInputStream(fileEntry);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] chunk = new byte[8192];
            int read;

            while ((read = inputStream.read(chunk)) >= 0) {
                buffer.write(chunk, 0, read);
            }

            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private boolean isMember(@Nullable Long userId, @Nullable Long workspaceId) {
        if (userId == null || workspaceId == null) {
            return false;
        }

        List<Workspace> workspaces = workspaceFacade.getUserWorkspaces(userId);

        return workspaces.stream()
            .map(Workspace::getId)
            .anyMatch(id -> Objects.equals(id, workspaceId));
    }

    private @Nullable Long fetchCurrentUserId() {
        return userService.fetchCurrentUser()
            .map(User::getId)
            .orElse(null);
    }

    private void checkMembership(@Nullable Long workspaceId) {
        Long userId = workspaceId == null ? null : fetchCurrentUserId();

        if (!isMember(userId, workspaceId)) {
            log.warn(
                "AssetFileFacade denying access (security-audit event): user {} attempted to access workspace {} they "
                    + "are not a member of",
                userId, workspaceId);

            throw new AccessDeniedException("Workspace is not accessible to the current user");
        }
    }

    private Long resolveOwningWorkspaceId(Long id) {
        AssetFile assetFile;

        try {
            assetFile = service.findById(id);
        } catch (IllegalArgumentException exception) {
            throw new AssetFileNotFoundException("Asset file %d not found".formatted(id));
        }

        Long workspaceId = assetFile.getWorkspaceId();

        if (workspaceId == null) {
            throw new AssetFileNotFoundException("Asset file %d not found".formatted(id));
        }

        return workspaceId;
    }

    private void checkMembershipOfOwner(Long id) {
        Long owningWorkspaceId = resolveOwningWorkspaceId(id);
        Long userId = fetchCurrentUserId();

        if (!isMember(userId, owningWorkspaceId)) {
            log.warn(
                "AssetFileFacade returning not-found (security-audit event): user {} attempted to access asset file "
                    + "{} owned by a workspace they are not a member of",
                userId, id);

            throw new AssetFileNotFoundException("Asset file %d not found".formatted(id));
        }
    }
}
