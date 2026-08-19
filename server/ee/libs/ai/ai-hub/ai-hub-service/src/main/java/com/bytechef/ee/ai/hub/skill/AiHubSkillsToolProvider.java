/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.skill;

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
 * Loads the current user's AI skills (the same set shown on the AI &gt; Skills page) into a single Spring AI Community
 * {@link SkillsTool} {@link ToolCallback} that the AI Hub agent can invoke — so a skill the user picks via the composer
 * slash-menu (e.g. {@code /research ...}) actually steers the run. The
 * {@link com.bytechef.ee.ai.hub.toolsearch.AiHubChatBindingToolCallbackResolver} unions this with the chat tools, user
 * connectors, and MCP server tools.
 *
 * <p>
 * Skills are scoped to the user by resolving the invocation context's {@code userId} to a login (rather than relying on
 * the {@code SecurityContext}, which isn't present on the agent's tool-resolution thread) and filtering by
 * {@link AiSkill#getCreatedBy()} — mirroring {@code AiSkillApiFacade}'s per-user filter. Each skill's archive is
 * downloaded, its {@code .md} files extracted to a temp directory, and the directory cached so repeated turns don't
 * re-download/unzip; cached directories are deleted on eviction.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AiHubSkillsToolProvider {

    private static final Logger log = LoggerFactory.getLogger(AiHubSkillsToolProvider.class);

    private static final int MAX_ZIP_ENTRIES = 1000;
    private static final long MAX_ZIP_ENTRY_SIZE = 5L * 1024 * 1024;

    private final AiSkillFacade aiSkillFacade;
    private final UserService userService;

    // Extracted skill directory per skillId. expireAfterWrite (not access) so an edited skill is picked up within the
    // TTL even if it's used every turn; the removal listener deletes the temp directory.
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
                // A single broken/empty skill archive must not strip the rest of the user's skills.
                log.warn("Skipping skill {} ({}) — could not extract it", skill.getId(), skill.getName(), exception);
            }
        }

        if (loaded == 0) {
            return List.of();
        }

        return List.of(builder.build());
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - The temp directory is created with a system-generated name; the only
     * caller-supplied part of the prefix is the numeric {@code skillId} (a {@code long}), which cannot contain
     * path-traversal characters. Extracted zip entries are additionally normalized and verified to stay within the temp
     * directory (see below).
     */
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
