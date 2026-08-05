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

package com.bytechef.component.ai.agent.utils.cluster;

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
 * A {@link WritableResource} bound to a single memory entry, identified by its slug {@code name} within a fixed
 * {@code (workspaceId, principalType, principalId, environment)} tenant. Reads render the row as a frontmatter
 * document; writes parse the document and create or update the row through {@link AiAutoMemoryService}. The backing
 * store ({@code ai_auto_memory}) remains the source of truth.
 *
 * @author Ivica Cardic
 */
final class AutoMemoryResource extends AbstractResource implements WritableResource {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final long workspaceId;
    private final AiAutoMemoryPrincipalType principalType;
    private final long principalId;
    private final int environment;
    private final String name;

    AutoMemoryResource(
        AiAutoMemoryService aiAutoMemoryService, long workspaceId, AiAutoMemoryPrincipalType principalType,
        long principalId, int environment, String name) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.workspaceId = workspaceId;
        this.principalType = principalType;
        this.principalId = principalId;
        this.environment = environment;
        this.name = name;
    }

    @Override
    public String getDescription() {
        return "AutoMemoryResource[" + name + "]";
    }

    @Override
    public boolean exists() {
        return aiAutoMemoryService
            .read(workspaceId, principalType, principalId, environment, name)
            .isPresent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        AiAutoMemory memory = aiAutoMemoryService
            .read(workspaceId, principalType, principalId, environment, name)
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

        AiAutoMemoryType parsedMemoryType = parsed.memoryType();
        String parsedTitle = parsed.title();

        AiAutoMemoryType memoryType = parsedMemoryType != null ? parsedMemoryType : AiAutoMemoryType.PROJECT;
        String title = parsedTitle != null && !parsedTitle.isBlank() ? parsedTitle : name;

        Optional<AiAutoMemory> existing =
            aiAutoMemoryService.read(workspaceId, principalType, principalId, environment, name);

        try {
            if (existing.isPresent()) {
                aiAutoMemoryService.update(
                    workspaceId, principalType, principalId, environment, name, title,
                    parsed.description(), memoryType, parsed.content());
            } else {
                aiAutoMemoryService.create(
                    workspaceId, principalType, principalId, environment, name, title,
                    parsed.description(), memoryType, parsed.content());
            }
        } catch (DuplicateAiAutoMemoryNameException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
