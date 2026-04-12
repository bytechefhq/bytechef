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

package com.bytechef.ai.agent.skill.facade;

import static com.bytechef.ai.agent.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.ai.agent.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.file.storage.AiAgentSkillFileStorage;
import com.bytechef.ai.agent.skill.service.AiAgentSkillService;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
class AiAgentSkillFacadeImpl implements AiAgentSkillFacade {

    // Reject uploaded skill archives larger than 10 MB
    private static final int MAX_SKILL_FILE_SIZE = 10 * 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(AiAgentSkillFacadeImpl.class);

    private final AiAgentSkillFileStorage aiAgentSkillFileStorage;
    private final AiAgentSkillService aiAgentSkillService;

    AiAgentSkillFacadeImpl(
        AiAgentSkillFileStorage aiAgentSkillFileStorage, AiAgentSkillService aiAgentSkillService) {

        this.aiAgentSkillFileStorage = aiAgentSkillFileStorage;
        this.aiAgentSkillService = aiAgentSkillService;
    }

    @Override
    public AiAgentSkill createAiAgentSkill(String name, @Nullable String description, String filename, byte[] bytes) {
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

        FileEntry fileEntry = aiAgentSkillFileStorage.storeAiAgentSkillFile(storageFilename, bytes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        aiAgentSkillFileStorage.deleteAiAgentSkillFile(fileEntry);
                    } catch (RuntimeException exception) {
                        logger.error(
                            "Failed to clean up skill file after rollback, fileEntry={}", fileEntry, exception);
                    }
                }
            }
        });

        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setName(skillName);
        aiAgentSkill.setDescription(skillDescription);
        aiAgentSkill.setSkillFileEntry(fileEntry);

        try {
            return aiAgentSkillService.createAiAgentSkill(aiAgentSkill);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (!isUniqueNameViolation(dataIntegrityViolationException)) {
                throw dataIntegrityViolationException;
            }

            logger.debug(
                "Unique name conflict during save for '{}', retrying with new suffix",
                aiAgentSkill.getName());

            aiAgentSkill.setName(generateUniqueName(aiAgentSkill.getName()));

            return aiAgentSkillService.createAiAgentSkill(aiAgentSkill);
        }
    }

    @Override
    public AiAgentSkill createAiAgentSkillFromInstructions(
        String name, @Nullable String description, String instructions) {

        Assert.hasText(name, "Skill name must not be blank");
        Assert.hasText(instructions, "Instructions must not be blank");

        byte[] zipBytes = createSkillZip(name, description, instructions);

        return createAiAgentSkill(name, description, name + ".skill", zipBytes);
    }

    @Override
    public void deleteAiAgentSkill(long id) {
        AiAgentSkill aiAgentSkill = aiAgentSkillService.getAiAgentSkill(id);

        FileEntry fileEntry = aiAgentSkill.getSkillFileEntry();

        logger.debug("Deleting agent skill id={}, name='{}', fileEntry={}", id, aiAgentSkill.getName(), fileEntry);

        aiAgentSkillService.deleteAiAgentSkill(id);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    aiAgentSkillFileStorage.deleteAiAgentSkillFile(fileEntry);
                } catch (RuntimeException exception) {
                    logger.error("Failed to delete skill file after DB commit, fileEntry={}", fileEntry, exception);
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getAiAgentSkillDownload(long id) {
        return getSkillZipBytes(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentSkill getAiAgentSkill(long id) {
        return aiAgentSkillService.getAiAgentSkill(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgentSkillDownload getAiAgentSkillWithDownload(long id) {
        AiAgentSkill aiAgentSkill = aiAgentSkillService.getAiAgentSkill(id);

        byte[] bytes = aiAgentSkillFileStorage.readAiAgentSkillFileBytes(aiAgentSkill.getSkillFileEntry());

        return new AiAgentSkillDownload(aiAgentSkill, bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public String getAiAgentSkillFileContent(long id, String path) {
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
    public List<String> getAiAgentSkillFilePaths(long id) {
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
    public List<AiAgentSkill> getAiAgentSkills() {
        return aiAgentSkillService.getAiAgentSkills();
    }

    @Override
    public AiAgentSkill updateAiAgentSkill(long id, String name, @Nullable String description) {
        Assert.hasText(name, "Skill name must not be blank");

        try {
            return aiAgentSkillService.updateAiAgentSkill(id, name, description);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (!isUniqueNameViolation(dataIntegrityViolationException)) {
                throw dataIntegrityViolationException;
            }

            throw new IllegalArgumentException(
                "A skill with the name '" + name + "' already exists", dataIntegrityViolationException);
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
        if (!aiAgentSkillService.existsByName(name)) {
            return name;
        }

        for (int suffix = 2; suffix <= 100; suffix++) {
            String candidateName = name + " (" + suffix + ")";

            if (!aiAgentSkillService.existsByName(candidateName)) {
                return candidateName;
            }
        }

        logger.warn("Exhausted 100 suffix attempts for skill name '{}', falling back to timestamp", name);

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
                        logger.warn("SKILL.md exceeds maximum allowed size, skipping frontmatter extraction");

                        return null;
                    }

                    String content = new String(entryBytes, StandardCharsets.UTF_8);

                    if (!content.startsWith("---")) {
                        logger.warn(
                            "SKILL.md does not contain frontmatter (no opening ---), using provided name/description");

                        return null;
                    }

                    int endIndex = content.indexOf("---", 3);

                    if (endIndex < 0) {
                        logger.warn("SKILL.md frontmatter has no closing ---, using provided name/description");

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

        logger.debug("No SKILL.md found in skill archive, using provided name/description");

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
        AiAgentSkill aiAgentSkill = aiAgentSkillService.getAiAgentSkill(id);

        return aiAgentSkillFileStorage.readAiAgentSkillFileBytes(aiAgentSkill.getSkillFileEntry());
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
