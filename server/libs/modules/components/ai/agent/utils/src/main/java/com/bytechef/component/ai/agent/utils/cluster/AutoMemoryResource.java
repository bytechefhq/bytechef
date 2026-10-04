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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
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
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.WritableResource;

/**
 * @author Ivica Cardic
 */
final class AutoMemoryResource extends AbstractResource implements WritableResource {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final AiAutoMemoryOwner owner;
    private final String name;
    private @Nullable Long baseId;
    private @Nullable Long baseVersion;

    AutoMemoryResource(AiAutoMemoryService aiAutoMemoryService, AiAutoMemoryOwner owner, String name) {
        this.aiAutoMemoryService = aiAutoMemoryService;
        this.owner = owner;
        this.name = name;
    }

    @Override
    public String getDescription() {
        return "AutoMemoryResource[" + owner + ", " + name + "]";
    }

    @Override
    public boolean exists() {
        return aiAutoMemoryService.read(owner, name)
            .isPresent();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        AiAutoMemory memory = aiAutoMemoryService.read(owner, name)
            .orElseThrow(() -> new IOException("Memory not found: " + name));

        setBase(memory);

        String rendered = AutoMemoryFrontmatter.render(
            memory.getName(), memory.getTitle(), memory.getDescription(), memory.getMemoryType(), memory.getContent());

        return new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream() {

            private boolean closed;

            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }

                closed = true;

                super.close();

                setBase(persist(toString(StandardCharsets.UTF_8)));
            }
        };
    }

    private void setBase(AiAutoMemory memory) {
        baseId = memory.getId();
        baseVersion = memory.getVersion();
    }

    private AiAutoMemory persist(String text) throws IOException {
        AutoMemoryFrontmatter.Parsed parsed;

        try {
            parsed = AutoMemoryFrontmatter.parse(text);
        } catch (IllegalArgumentException exception) {
            throw new IOException(exception.getMessage(), exception);
        }

        String parsedName = parsed.name();

        if (parsedName != null && !parsedName.equals(name)) {
            throw new IOException(
                "The frontmatter name '" + parsedName + "' does not match the entry path '" + name
                    + "'. Use MemoryRename to rename an entry.");
        }

        try {
            if (baseId != null && baseVersion != null) {
                return update(baseId, baseVersion, parsed);
            }

            if (aiAutoMemoryService.read(owner, name)
                .isPresent()) {

                throw new IOException(
                    "Memory entry '" + name + "' already exists. View it and use MemoryStrReplace to change it.");
            }

            AutoMemoryResourceResolver.verifyMemoryName(name);

            AiAutoMemoryType parsedMemoryType = parsed.memoryType();
            String parsedTitle = parsed.title();

            return aiAutoMemoryService.create(
                owner, name, parsedTitle == null ? name : parsedTitle, parsed.description(),
                parsedMemoryType == null ? AiAutoMemoryType.PROJECT : parsedMemoryType, parsed.content());
        } catch (AiAutoMemoryConcurrentModificationException | DuplicateAiAutoMemoryNameException
            | IllegalArgumentException exception) {

            throw new IOException(exception.getMessage(), exception);
        }
    }

    private AiAutoMemory update(long memoryId, long expectedVersion, AutoMemoryFrontmatter.Parsed parsed)
        throws IOException {

        AiAutoMemoryPatch patch = new AiAutoMemoryPatch(
            parsed.title(), Objects.requireNonNullElse(parsed.description(), ""), parsed.memoryType(),
            parsed.content());

        try {
            return aiAutoMemoryService.updateById(owner, memoryId, expectedVersion, patch);
        } catch (AiAutoMemoryNotFoundException exception) {
            throw new IOException(
                "Memory entry '" + name + "' was deleted by someone else since it was read.", exception);
        }
    }
}
