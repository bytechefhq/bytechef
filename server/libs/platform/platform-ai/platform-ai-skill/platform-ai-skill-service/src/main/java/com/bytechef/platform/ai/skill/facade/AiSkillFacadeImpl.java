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

package com.bytechef.platform.ai.skill.facade;

import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRIES;
import static com.bytechef.platform.ai.skill.SkillArchiveConstants.MAX_ZIP_ENTRY_SIZE;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@SuppressFBWarnings(
    value = "REDOS",
    justification = "SKILL_NAME_PATTERN uses fixed separators between character classes — runs in linear time")
class AiSkillFacadeImpl implements AiSkillFacade {

    private static final int MAX_SKILL_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_SKILL_NAME_LENGTH = 64;
    private static final int MAX_SKILL_DESCRIPTION_LENGTH = 1024;
    private static final Pattern SKILL_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]+([- ][a-zA-Z0-9]+)*$");
    private static final Pattern FRONTMATTER_BLOCK_PATTERN = Pattern.compile("^---\n([\\s\\S]*?)\n---\n([\\s\\S]*)$");
    private static final String SKILL_MD_FILENAME = "SKILL.md";

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

        validateSkillName(skillName);
        validateSkillDescription(skillDescription);

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
        String name, @Nullable String description, String instructions,
        @Nullable Map<String, String> additionalFiles) {

        Assert.hasText(name, "Skill name must not be blank");
        Assert.hasText(instructions, "Instructions must not be blank");

        validateAdditionalFilePaths(additionalFiles);

        String body = stripLeadingFrontmatter(instructions);

        Map<String, String> resolvedAdditionalFiles =
            additionalFiles != null ? additionalFiles : Collections.emptyMap();

        byte[] zipBytes = createSkillZip(name, description, body, resolvedAdditionalFiles);

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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
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

                String name = zipEntry.getName();

                if (name.equals(path)) {
                    byte[] entryBytes = zipInputStream.readNBytes(MAX_ZIP_ENTRY_SIZE + 1);

                    if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                        throw new IllegalArgumentException("File exceeds maximum allowed size: " + path);
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

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        if (contentBytes.length > MAX_ZIP_ENTRY_SIZE) {
            throw new IllegalArgumentException(
                "Skill file content exceeds maximum allowed size of " + (MAX_ZIP_ENTRY_SIZE / 1024 / 1024) + " MB");
        }

        String targetPath = (path != null && !path.isBlank()) ? path : SKILL_MD_FILENAME;

        if (targetPath.contains("..") || targetPath.startsWith("/")) {
            throw new IllegalArgumentException(
                "Path must not contain path traversal sequences or be absolute: " + targetPath);
        }

        if (SKILL_MD_FILENAME.equalsIgnoreCase(targetPath)) {
            validateSkillMdFrontmatter(content);
        }

        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        FileEntry oldFileEntry = aiSkill.getSkillFile();

        byte[] existingZipBytes = aiSkillFileStorage.readAiSkillFileBytes(oldFileEntry);
        byte[] zipBytes = replaceZipEntry(existingZipBytes, targetPath, contentBytes);

        if (zipBytes.length > MAX_SKILL_FILE_SIZE) {
            throw new IllegalArgumentException(
                "Updated skill archive exceeds maximum allowed size of "
                    + (MAX_SKILL_FILE_SIZE / 1024 / 1024) + " MB");
        }

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
    public AiSkill createAdditionalFilesInSkill(long id, Map<String, String> additionalFiles) {
        Assert.notEmpty(additionalFiles, "additionalFiles must not be null or empty");

        validateAdditionalFilePaths(additionalFiles);

        AiSkill aiSkill = aiSkillService.getAiSkill(id);

        FileEntry oldFileEntry = aiSkill.getSkillFile();

        byte[] zipBytes = aiSkillFileStorage.readAiSkillFileBytes(oldFileEntry);

        for (Map.Entry<String, String> entry : additionalFiles.entrySet()) {
            byte[] contentBytes = entry.getValue()
                .getBytes(StandardCharsets.UTF_8);

            if (contentBytes.length > MAX_ZIP_ENTRY_SIZE) {
                throw new IllegalArgumentException(
                    "File content exceeds maximum allowed size: " + entry.getKey());
            }

            zipBytes = replaceZipEntry(zipBytes, entry.getKey(), contentBytes);
        }

        if (zipBytes.length > MAX_SKILL_FILE_SIZE) {
            throw new IllegalArgumentException(
                "Updated skill archive exceeds maximum allowed size of "
                    + (MAX_SKILL_FILE_SIZE / 1024 / 1024) + " MB");
        }

        FileEntry newFileEntry = aiSkillFileStorage.storeAiSkillFile(
            toSkillFilename(aiSkill.getName()), zipBytes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                try {
                    aiSkillFileStorage.deleteAiSkillFile(oldFileEntry);
                } catch (RuntimeException exception) {
                    log.error(
                        "Failed to delete old skill file after adding files, fileEntry={}", oldFileEntry, exception);
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

        validateSkillName(name);
        validateSkillDescription(description);

        AiSkill updatedAiSkill;

        try {
            updatedAiSkill = aiSkillService.updateAiSkill(id, name, description);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            if (!isUniqueNameViolation(dataIntegrityViolationException)) {
                throw dataIntegrityViolationException;
            }

            throw new IllegalArgumentException(
                "A skill with the name '" + name + "' already exists", dataIntegrityViolationException);
        }

        syncSkillMdFrontmatter(id, name, description);

        return updatedAiSkill;
    }

    private void syncSkillMdFrontmatter(long id, String name, @Nullable String description) {
        try {
            String currentContent = getAiSkillFileContent(id, SKILL_MD_FILENAME);
            String updatedContent = applyFrontmatterFields(currentContent, name, description);

            if (!updatedContent.equals(currentContent)) {
                updateAiSkillContent(id, SKILL_MD_FILENAME, updatedContent);
            }
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to sync SKILL.md frontmatter for skill id={} (name='{}'); the DB row was updated but the archive may now disagree with the spec",
                id, name, exception);
        }
    }

    private String applyFrontmatterFields(String content, String name, @Nullable String description) {
        String nameLine = "name: \"" + escapeYamlValue(name) + "\"";
        String descLine = "description: \"" + escapeYamlValue(description != null ? description : "") + "\"";

        Matcher frontmatterMatcher = FRONTMATTER_BLOCK_PATTERN.matcher(content);

        if (!frontmatterMatcher.find()) {
            return "---\n" + nameLine + "\n" + descLine + "\n---\n\n" + content;
        }

        String frontmatterBlock = frontmatterMatcher.group(1);
        String body = frontmatterMatcher.group(2);

        String updatedBlock = replaceOrAppendKey(frontmatterBlock, "name", nameLine);

        updatedBlock = replaceOrAppendKey(updatedBlock, "description", descLine);

        return "---\n" + updatedBlock + "\n---\n\n" + body.stripLeading();
    }

    private String replaceOrAppendKey(String block, String key, String newLine) {
        Pattern keyPattern = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*:.*$", Pattern.MULTILINE);
        Matcher matcher = keyPattern.matcher(block);

        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(newLine));
        }

        return block + "\n" + newLine;
    }

    private byte[] replaceZipEntry(byte[] existingZipBytes, String targetPath, byte[] newContentBytes) {
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

        entries.put(targetPath, newContentBytes);

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

    private String generateUniqueName(String name) {
        if (!aiSkillService.existsByName(name)) {
            return name;
        }

        for (int suffix = 2; suffix <= 100; suffix++) {
            String candidateName = name + "-" + suffix;

            if (!aiSkillService.existsByName(candidateName)) {
                return candidateName;
            }
        }

        log.warn("Exhausted 100 suffix attempts for skill name '{}', falling back to timestamp", name);

        return name + "-" + System.currentTimeMillis();
    }

    private void validateSkillName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }

        if (name.length() > MAX_SKILL_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "Skill name must be at most " + MAX_SKILL_NAME_LENGTH + " characters: '" + name + "'");
        }

        if (!SKILL_NAME_PATTERN.matcher(name)
            .matches()) {

            throw new IllegalArgumentException(
                "Skill name must contain only letters, digits, single hyphens, or single spaces — " +
                    "no leading, trailing, or consecutive separators (got '" + name + "')");
        }
    }

    private void validateSkillDescription(@Nullable String description) {
        if (description != null && description.length() > MAX_SKILL_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "Skill description must be at most " + MAX_SKILL_DESCRIPTION_LENGTH + " characters");
        }
    }

    private void validateSkillMdFrontmatter(String content) {
        Map<String, String> frontmatter = parseFrontmatter(content);

        if (frontmatter == null) {
            throw new IllegalArgumentException(
                "SKILL.md must start with a YAML frontmatter block delimited by --- lines");
        }

        String name = frontmatter.get("name");

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SKILL.md frontmatter must include a 'name' field");
        }

        validateSkillName(name);

        String description = frontmatter.get("description");

        if (description != null) {
            validateSkillDescription(description);
        }
    }

    @Nullable
    private Map<String, String> parseFrontmatter(String content) {
        Matcher frontmatterMatcher = FRONTMATTER_BLOCK_PATTERN.matcher(content);

        if (!frontmatterMatcher.find()) {
            return null;
        }

        return parseFrontmatterFields(frontmatterMatcher.group(1)
            .trim());
    }

    private Map<String, String> parseFrontmatterFields(String frontmatterBlock) {
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

    private String stripLeadingFrontmatter(String content) {
        if (!content.startsWith("---\n")) {
            return content;
        }

        int closingDelimStart = content.indexOf("\n---\n", 4);

        if (closingDelimStart < 0) {
            return content;
        }

        return content.substring(closingDelimStart + "\n---\n".length())
            .stripLeading();
    }

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

                    Map<String, String> frontmatter = parseFrontmatter(content);

                    if (frontmatter == null) {
                        log.warn(
                            "SKILL.md has no valid YAML frontmatter block (must be delimited by --- on their own lines), "
                                + "using provided name/description");
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

    private byte[] createSkillZip(
        String name, @Nullable String description, String instructions,
        Map<String, String> additionalFiles) {

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

            for (Map.Entry<String, String> entry : additionalFiles.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue()
                    .getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to create skill zip", ioException);
        }
    }

    private void validateAdditionalFilePaths(@Nullable Map<String, String> additionalFiles) {
        if (additionalFiles == null) {
            return;
        }

        for (String path : additionalFiles.keySet()) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Additional file path must not be blank");
            }

            if (path.contains("..") || path.startsWith("/")) {
                throw new IllegalArgumentException(
                    "Additional file path must not contain path traversal sequences or be absolute: " + path);
            }
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
