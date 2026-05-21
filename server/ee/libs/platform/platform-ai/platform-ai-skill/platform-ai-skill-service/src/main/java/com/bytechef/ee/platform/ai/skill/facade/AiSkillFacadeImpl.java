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

package com.bytechef.ee.platform.ai.skill.facade;

import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.ee.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.ee.platform.ai.skill.service.AiSkillService;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
class AiSkillFacadeImpl implements AiSkillFacade {

    // Reject uploaded skill archives larger than 10 MB
    private static final int MAX_SKILL_FILE_SIZE = 10 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(AiSkillFacadeImpl.class);

    private final AiSkillFileStorage aiSkillFileStorage;
    private final AiSkillService aiSkillService;

    AiSkillFacadeImpl(
        AiSkillFileStorage aiSkillFileStorage, AiSkillService aiSkillService) {

        this.aiSkillFileStorage = aiSkillFileStorage;
        this.aiSkillService = aiSkillService;
    }

    @Override
    public AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes) {
        Assert.hasText(name, "Skill name must not be blank");
        Assert.hasText(filename, "Filename must not be blank");

        if (bytes.length > MAX_SKILL_FILE_SIZE) {
            throw new IllegalArgumentException(
                "Skill file exceeds maximum allowed size of " + (MAX_SKILL_FILE_SIZE / 1024 / 1024) + " MB");
        }

        validateZipArchive(bytes);

        String skillName = name;
        String skillDescription = description;

        Map<String, String> frontmatter = extractFrontmatter(bytes);

        if (frontmatter != null) {
            String frontmatterName = frontmatter.get("name");

            if (frontmatterName != null && !frontmatterName.isBlank()) {
                skillName = frontmatterName;
            }

            String frontmatterDescription = frontmatter.get("description");

            if (frontmatterDescription != null && !frontmatterDescription.isBlank()) {
                skillDescription = frontmatterDescription;
            }
        }

        skillName = generateUniqueName(skillName);

        String storageFilename = toSkillFilename(filename);

        FileEntry fileEntry = aiSkillFileStorage.storeAiSkillFile(storageFilename, bytes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        aiSkillFileStorage.deleteAiSkillFile(fileEntry);
                    } catch (RuntimeException exception) {
                        log.error(
                            "Failed to clean up skill file after rollback, fileEntry={}", fileEntry, exception);
                    }
                }
            }
        });

        AiSkill aiSkill = new AiSkill();

        aiSkill.setName(skillName);
        aiSkill.setDescription(skillDescription);
        aiSkill.setSkillFile(fileEntry);

        try {
            return aiSkillService.createAiSkill(aiSkill);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (!isUniqueNameViolation(dataIntegrityViolationException)) {
                throw dataIntegrityViolationException;
            }

            log.debug(
                "Unique name conflict during save for '{}', retrying with new suffix",
                aiSkill.getName());

            aiSkill.setName(generateUniqueName(aiSkill.getName()));

            return aiSkillService.createAiSkill(aiSkill);
        }
    }

    @Override
    public AiSkill createAiSkillFromInstructions(
        String name, @Nullable String description, String instructions) {

        Assert.hasText(name, "Skill name must not be blank");
        Assert.hasText(instructions, "Instructions must not be blank");

        byte[] zipBytes = createSkillZip(name, description, instructions);

        return createAiSkill(name, description, name + ".skill", zipBytes);
    }

    @Override
    public void deleteAiSkill(long id) {
        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        FileEntry fileEntry = aiSkill.getSkillFile();

        log.debug("Deleting agent skill id={}, name='{}', fileEntry={}", id, aiSkill.getName(), fileEntry);

        aiSkillService.deleteAiSkill(id);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    aiSkillFileStorage.deleteAiSkillFile(fileEntry);
                } catch (RuntimeException exception) {
                    log.error("Failed to delete skill file after DB commit, fileEntry={}", fileEntry, exception);
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAiSkillDownload(long id) {
        return getSkillZipBytes(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSkill getAiSkill(long id) {
        return aiSkillService.getAiSkill(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiSkillDownload getAiSkillWithDownload(long id) {
        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        byte[] bytes = aiSkillFileStorage.readAiSkillFileBytes(aiSkill.getSkillFile());

        return new AiSkillDownload(aiSkill, bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAiSkillFileContent(long id, String path) {
        Assert.hasText(path, "File path must not be blank");

        if (path.contains("..") || path.startsWith("/")) {
            throw new IllegalArgumentException(
                "Path must not contain path traversal sequences or be absolute: " + path);
        }

        byte[] zipBytes = getSkillZipBytes(id);

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (zipEntry.getName()
                    .equals(path)) {

                    byte[] entryBytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                        throw new IllegalArgumentException(
                            "File exceeds maximum allowed size: " + path);
                    }

                    return new String(entryBytes, StandardCharsets.UTF_8);
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read skill file: " + path, ioException);
        }

        throw new IllegalArgumentException("File not found in skill archive: " + path);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAiSkillFilePaths(long id) {
        byte[] zipBytes = getSkillZipBytes(id);

        List<String> paths = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                String entryName = zipEntry.getName();

                if (!zipEntry.isDirectory() && !entryName.contains("..") && !entryName.startsWith("/")) {
                    paths.add(entryName);
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to list skill files", ioException);
        }

        return paths;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiSkill> getAiSkills() {
        return aiSkillService.getAiSkills();
    }

    @Override
    public AiSkill updateAiSkillContent(long id, @Nullable String path, String content) {
        Assert.hasText(content, "Content must not be blank");

        String targetPath = (path != null && !path.isBlank()) ? path : "SKILL.md";

        if (targetPath.contains("..") || targetPath.startsWith("/")) {
            throw new IllegalArgumentException(
                "Path must not contain path traversal sequences or be absolute: " + targetPath);
        }

        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        FileEntry oldFileEntry = aiSkill.getSkillFile();

        byte[] existingZipBytes = aiSkillFileStorage.readAiSkillFileBytes(oldFileEntry);
        byte[] zipBytes = replaceZipEntry(existingZipBytes, targetPath, content);

        FileEntry newFileEntry = aiSkillFileStorage.storeAiSkillFile(
            toSkillFilename(aiSkill.getName()), zipBytes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    aiSkillFileStorage.deleteAiSkillFile(oldFileEntry);
                } catch (RuntimeException exception) {
                    log.error(
                        "Failed to delete old skill file after content update, fileEntry={}", oldFileEntry, exception);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        aiSkillFileStorage.deleteAiSkillFile(newFileEntry);
                    } catch (RuntimeException exception) {
                        log.error(
                            "Failed to clean up new skill file after rollback, fileEntry={}", newFileEntry, exception);
                    }
                }
            }
        });

        return aiSkillService.updateAiSkillFile(id, newFileEntry);
    }

    @Override
    public AiSkill updateAiSkill(long id, String name, @Nullable String description) {
        Assert.hasText(name, "Skill name must not be blank");

        try {
            return aiSkillService.updateAiSkill(id, name, description);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (!isUniqueNameViolation(dataIntegrityViolationException)) {
                throw dataIntegrityViolationException;
            }

            throw new IllegalArgumentException(
                "A skill with the name '" + name + "' already exists", dataIntegrityViolationException);
        }
    }

    /**
     * Reads all entries from the existing zip, replaces (or adds) the entry at {@code targetPath} with
     * {@code newContent}, and returns the bytes of the new zip. All other entries are copied verbatim.
     */
    private byte[] replaceZipEntry(byte[] existingZipBytes, String targetPath, String newContent) {
        Map<String, byte[]> entries = new LinkedHashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(existingZipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (!zipEntry.isDirectory()) {
                    entries.put(zipEntry.getName(), zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1));
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read existing skill archive", ioException);
        }

        entries.put(targetPath, newContent.getBytes(StandardCharsets.UTF_8));

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to write updated skill archive", ioException);
        }
    }

    private boolean isUniqueNameViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();

        if (cause instanceof java.sql.SQLException sqlException) {
            // PostgreSQL unique violation = 23505; H2 also uses 23505
            return "23505".equals(sqlException.getSQLState());
        }

        String message = exception.getMessage();

        return message != null && message.contains("agent_skill_name");
    }

    /** Appends a numeric suffix (2)-(100) to avoid name collisions; falls back to a timestamp suffix if all taken. */
    private String generateUniqueName(String name) {
        if (!aiSkillService.existsByName(name)) {
            return name;
        }

        for (int suffix = 2; suffix <= 100; suffix++) {
            String candidateName = name + " (" + suffix + ")";

            if (!aiSkillService.existsByName(candidateName)) {
                return candidateName;
            }
        }

        log.warn("Exhausted 100 suffix attempts for skill name '{}', falling back to timestamp", name);

        return name + " (" + System.currentTimeMillis() + ")";
    }

    /**
     * Extracts YAML frontmatter (key-value pairs between opening and closing --- delimiters) from SKILL.md inside the
     * zip archive. Returns null if no SKILL.md is found or the frontmatter is malformed.
     */
    @Nullable
    private Map<String, String> extractFrontmatter(byte[] zipBytes) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            int entryCount = 0;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > MAX_ZIP_ENTRIES) {
                    throw new IllegalArgumentException(
                        "Skill archive exceeds maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                }

                if (zipEntry.getName()
                    .equalsIgnoreCase("SKILL.md")) {

                    byte[] entryBytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                        log.warn("SKILL.md exceeds maximum allowed size, skipping frontmatter extraction");

                        return null;
                    }

                    String content = new String(entryBytes, StandardCharsets.UTF_8);

                    if (!content.startsWith("---")) {
                        log.warn(
                            "SKILL.md does not contain frontmatter (no opening ---), using provided name/description");

                        return null;
                    }

                    int endIndex = content.indexOf("---", 3);

                    if (endIndex < 0) {
                        log.warn("SKILL.md frontmatter has no closing ---, using provided name/description");

                        return null;
                    }

                    String frontmatterBlock = content.substring(3, endIndex)
                        .trim();

                    Map<String, String> frontmatter = new HashMap<>();

                    for (String line : frontmatterBlock.split("\n")) {
                        int colonIndex = line.indexOf(':');

                        if (colonIndex > 0) {
                            String key = line.substring(0, colonIndex)
                                .trim();
                            String value = line.substring(colonIndex + 1)
                                .trim();

                            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1)
                                    .replace("\\\"", "\"")
                                    .replace("\\n", "\n")
                                    .replace("\\r", "\r")
                                    .replace("\\\\", "\\");
                            }

                            frontmatter.put(key, value);
                        }
                    }

                    return frontmatter;
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                "Failed to read the uploaded skill archive. The file may be corrupt.", ioException);
        }

        log.debug("No SKILL.md found in skill archive, using provided name/description");

        return null;
    }

    private byte[] createSkillZip(String name, @Nullable String description, String instructions) {
        String skillMdContent = "---\n" +
            "name: \"" + escapeYamlValue(name) + "\"\n" +
            "description: \"" + escapeYamlValue(description != null ? description : "") + "\"\n" +
            "---\n\n" +
            instructions;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry("SKILL.md"));
            zipOutputStream.write(skillMdContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to create skill zip", ioException);
        }
    }

    private byte[] getSkillZipBytes(long id) {
        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        return aiSkillFileStorage.readAiSkillFileBytes(aiSkill.getSkillFile());
    }

    private String escapeYamlValue(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    // Normalizes uploaded filenames to use the .skill extension for consistent storage
    private String toSkillFilename(String filename) {
        if (filename.endsWith(".zip")) {
            return filename.substring(0, filename.length() - 4) + ".skill";
        }

        if (filename.endsWith(".skill")) {
            return filename;
        }

        return filename + ".skill";
    }

    private void validateZipArchive(byte[] bytes) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            if (zipInputStream.getNextEntry() == null) {
                throw new IllegalArgumentException(
                    "The uploaded file is not a valid zip archive or contains no entries");
            }
        } catch (IOException ioException) {
            throw new IllegalArgumentException(
                "The uploaded file is not a valid zip archive", ioException);
        }
    }
}
