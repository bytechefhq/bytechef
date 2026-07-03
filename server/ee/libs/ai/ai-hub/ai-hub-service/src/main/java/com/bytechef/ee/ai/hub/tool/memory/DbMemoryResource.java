/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.AutoMemoryFrontmatter;
import com.bytechef.platform.ai.auto.memory.DuplicateAiAutoMemoryNameException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

/**
 * A {@link WritableResource} bound to a single memory entry, identified by its slug {@code name} within a
 * {@code (workspaceId, userId, environment)} tenant. Reads render the row as a frontmatter document; writes parse the
 * document and create or update the row through {@link AiAutoMemoryService}. The backing store ({@code ai_auto_memory})
 * remains the source of truth.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class DbMemoryResource extends AbstractResource implements WritableResource {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final long workspaceId;
    private final long userId;
    private final int environment;
    private final String name;

    DbMemoryResource(
        AiAutoMemoryService aiAutoMemoryService, long workspaceId, long userId, int environment, String name) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.environment = environment;
        this.name = name;
    }

    @Override
    public String getDescription() {
        return "DbMemoryResource[" + name + "]";
    }

    @Override
    public boolean exists() {
        return aiAutoMemoryService.read(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, name)
            .isPresent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        AiAutoMemory memory = aiAutoMemoryService
            .read(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, name)
            .orElseThrow(() -> new IOException("Memory not found: " + name));

        String rendered = AutoMemoryFrontmatter.render(
            memory.getName(), memory.getTitle(), memory.getDescription(), memory.getMemoryType(), memory.getContent());

        return new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();

                persist(toString(StandardCharsets.UTF_8));
            }
        };
    }

    private void persist(String text) throws IOException {
        AutoMemoryFrontmatter.Parsed parsed = AutoMemoryFrontmatter.parse(text);

        AiAutoMemoryType memoryType = parsed.memoryType() != null ? parsed.memoryType() : AiAutoMemoryType.PROJECT;
        String title = parsed.title() != null && !parsed.title()
            .isBlank() ? parsed.title() : name;

        Optional<AiAutoMemory> existing =
            aiAutoMemoryService.read(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, name);

        try {
            if (existing.isPresent()) {
                aiAutoMemoryService.update(
                    workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, name, title,
                    parsed.description(), memoryType, parsed.content());
            } else {
                aiAutoMemoryService.create(
                    workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, name, title,
                    parsed.description(), memoryType, parsed.content());
            }
        } catch (DuplicateAiAutoMemoryNameException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
