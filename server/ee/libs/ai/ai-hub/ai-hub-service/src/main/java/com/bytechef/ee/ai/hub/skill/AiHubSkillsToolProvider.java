/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.skill;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.user.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubSkillsToolProvider {

    private static final Logger log = LoggerFactory.getLogger(AiHubSkillsToolProvider.class);

    private static final int MAX_ZIP_ENTRIES = 1000;
    private static final long MAX_ZIP_ENTRY_SIZE = 5L * 1024 * 1024;

    private final AiSkillFacade aiSkillFacade;
    private final UserService userService;

    private final Cache<Long, Path> skillDirectoryCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(Duration.ofMinutes(10))
        .<Long, Path>removalListener((skillId, directory, cause) -> deleteQuietly(directory))
        .build();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubSkillsToolProvider(AiSkillFacade aiSkillFacade, UserService userService) {
        this.aiSkillFacade = aiSkillFacade;
        this.userService = userService;
    }

    public List<ToolCallback> resolve(long userId) {
        String login;

        try {
            login = userService.getUser(userId)
                .getLogin();
        } catch (RuntimeException exception) {
            log.warn("Could not resolve user {} for skills; skipping skill tools", userId, exception);

            return List.of();
        }

        List<AiSkill> skills = aiSkillFacade.getAiSkills()
            .stream()
            .filter(skill -> login.equals(skill.getCreatedBy()))
            .toList();

        if (skills.isEmpty()) {
            return List.of();
        }

        SkillsTool.Builder builder = SkillsTool.builder();
        int loaded = 0;

        for (AiSkill skill : skills) {
            try {
                Path skillDirectory = skillDirectoryCache.get(skill.getId(), this::extractSkill);

                if (skillDirectory != null) {
                    builder.addSkillsDirectory(skillDirectory.toString());

                    loaded++;
                }
            } catch (RuntimeException exception) {
                log.warn("Skipping skill {} ({}) — could not extract it", skill.getId(), skill.getName(), exception);
            }
        }

        if (loaded == 0) {
            return List.of();
        }

        return List.of(builder.build());
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private Path extractSkill(long skillId) {
        byte[] zipBytes = aiSkillFacade.getAiSkillDownload(skillId);

        try {
            Path tempDirectory = Files.createTempDirectory("bytechef-aihub-skill-" + skillId + "-");

            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry zipEntry;
                int entryCount = 0;

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    if (++entryCount > MAX_ZIP_ENTRIES) {
                        throw new IllegalArgumentException(
                            "Skill archive exceeds the maximum allowed number of entries (" + MAX_ZIP_ENTRIES + ")");
                    }

                    if (zipEntry.isDirectory()) {
                        continue;
                    }

                    String entryName = zipEntry.getName();

                    if (!entryName.toLowerCase()
                        .endsWith(".md")) {

                        continue;
                    }

                    byte[] entryBytes = zipInputStream.readNBytes((int) MAX_ZIP_ENTRY_SIZE + 1);

                    if (entryBytes.length > MAX_ZIP_ENTRY_SIZE) {
                        throw new IllegalArgumentException("Skill archive entry '" + entryName + "' is too large");
                    }

                    Path targetPath = tempDirectory.resolve(entryName)
                        .normalize();

                    if (!targetPath.startsWith(tempDirectory)) {
                        throw new IllegalArgumentException("Zip entry path traversal detected: " + entryName);
                    }

                    Path parentPath = targetPath.getParent();

                    if (parentPath != null) {
                        Files.createDirectories(parentPath);
                    }

                    Files.write(targetPath, entryBytes);
                }
            }

            return tempDirectory;
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to extract skill " + skillId, ioException);
        }
    }

    private static void deleteQuietly(@Nullable Path directory) {
        if (directory == null) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(AiHubSkillsToolProvider::deletePathQuietly);
        } catch (IOException exception) {
            log.debug("Failed to delete skill temp directory {}", directory, exception);
        }
    }

    private static void deletePathQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.debug("Failed to delete {}", path, exception);
        }
    }
}
