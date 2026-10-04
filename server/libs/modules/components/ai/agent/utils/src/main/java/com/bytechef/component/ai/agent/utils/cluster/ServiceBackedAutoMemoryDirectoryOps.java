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

import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryConcurrentModificationException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AutoMemoryFrontmatter;
import com.bytechef.platform.ai.auto.memory.DuplicateAiAutoMemoryNameException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public final class ServiceBackedAutoMemoryDirectoryOps implements AutoMemoryDirectoryOps {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final AiAutoMemoryOwner owner;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ServiceBackedAutoMemoryDirectoryOps(AiAutoMemoryService aiAutoMemoryService, AiAutoMemoryOwner owner) {
        this.aiAutoMemoryService = Objects.requireNonNull(aiAutoMemoryService, "aiAutoMemoryService");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public String list(String path) {
        List<AiAutoMemory> memories = aiAutoMemoryService.list(owner, null);

        if (memories.isEmpty()) {
            return "MEMORY index is empty. Create entries with MemoryCreate.";
        }

        StringBuilder stringBuilder = new StringBuilder("MEMORY index (");

        stringBuilder.append(memories.size())
            .append(" entries):\n");

        for (AiAutoMemory memory : memories) {
            stringBuilder.append("- ")
                .append(memory.getName())
                .append(".md — [")
                .append(memory.getMemoryType()
                    .name())
                .append("] ")
                .append(AutoMemoryFrontmatter.toSingleLine(memory.getTitle()));

            String description = memory.getDescription();

            if (description != null && !description.isBlank()) {
                stringBuilder.append(" — ")
                    .append(AutoMemoryFrontmatter.toSingleLine(description));
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    @Override
    public boolean exists(String relativePath) {
        return aiAutoMemoryService.read(owner, AutoMemoryResourceResolver.toMemoryName(relativePath))
            .isPresent();
    }

    @Override
    public void delete(String relativePath) throws IOException {
        try {
            aiAutoMemoryService.delete(owner, AutoMemoryResourceResolver.toMemoryName(relativePath));
        } catch (AiAutoMemoryNotFoundException exception) {
            throw new IOException("Memory entry does not exist: " + relativePath, exception);
        } catch (AiAutoMemoryConcurrentModificationException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }

    @Override
    public void rename(String oldRelativePath, String newRelativePath) throws IOException {
        String newName = AutoMemoryResourceResolver.toMemoryName(newRelativePath);

        AutoMemoryResourceResolver.verifyMemoryName(newName);

        try {
            aiAutoMemoryService.rename(owner, AutoMemoryResourceResolver.toMemoryName(oldRelativePath), newName);
        } catch (AiAutoMemoryNotFoundException exception) {
            throw new IOException("Memory entry does not exist: " + oldRelativePath, exception);
        } catch (AiAutoMemoryConcurrentModificationException | DuplicateAiAutoMemoryNameException exception) {
            throw new IOException(exception.getMessage(), exception);
        }
    }
}
