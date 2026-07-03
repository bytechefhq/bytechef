/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.AutoMemoryFrontmatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DbAutoMemorySeamTest {

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);

    private ToolContext toolContext() {
        AiHubToolInvocationContext context = new AiHubToolInvocationContext(1L, 2L, (short) 0, "prompt", 0L, "thread");

        return new ToolContext(context.toToolContext());
    }

    @Test
    void testDirectoryOpsExistsDelegatesToRead() {
        when(aiAutoMemoryService.read(1L, AiAutoMemoryPrincipalType.USER, 2L, 0, "note"))
            .thenReturn(Optional.of(mock(AiAutoMemory.class)));

        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        assertThat(directoryOps.exists("note.md", toolContext())).isTrue();
    }

    @Test
    void testDirectoryOpsDeleteStripsMdExtension() {
        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        directoryOps.delete("note.md", toolContext());

        verify(aiAutoMemoryService).delete(1L, AiAutoMemoryPrincipalType.USER, 2L, 0, "note");
    }

    @Test
    void testDirectoryOpsRenameStripsExtensions() {
        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        directoryOps.rename("old.md", "new.md", toolContext());

        verify(aiAutoMemoryService).rename(1L, AiAutoMemoryPrincipalType.USER, 2L, 0, "old", "new");
    }

    @Test
    void testDirectoryOpsListRendersIndex() {
        AiAutoMemory memory = mock(AiAutoMemory.class);

        when(memory.getName()).thenReturn("user_profile");
        when(memory.getTitle()).thenReturn("User Profile");
        when(memory.getMemoryType()).thenReturn(AiAutoMemoryType.USER);
        when(memory.getDescription()).thenReturn("who the user is");
        when(aiAutoMemoryService.listByPrincipalAndWorkspace(1L, AiAutoMemoryPrincipalType.USER, 2L, 0))
            .thenReturn(List.of(memory));

        DbAutoMemoryDirectoryOps directoryOps = new DbAutoMemoryDirectoryOps(aiAutoMemoryService);

        String index = directoryOps.list("MEMORY.md", toolContext());

        assertThat(index).contains("user_profile.md");
        assertThat(index).contains("User Profile");
        assertThat(index).contains("who the user is");
    }

    @Test
    void testResolverWritesCreateThroughService() throws Exception {
        when(aiAutoMemoryService.read(1L, AiAutoMemoryPrincipalType.USER, 2L, 0, "user_profile"))
            .thenReturn(Optional.empty());

        DbMemoryResourceResolver resolver = new DbMemoryResourceResolver(aiAutoMemoryService);

        String text = AutoMemoryFrontmatter.render(
            "user_profile", "User Profile", "who", AiAutoMemoryType.USER, "body");

        try (var outputStream = resolver.resolve("user_profile.md", toolContext())
            .getOutputStream()) {

            outputStream.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        verify(aiAutoMemoryService).create(
            eq(1L), eq(AiAutoMemoryPrincipalType.USER), eq(2L), eq(0), eq("user_profile"), eq("User Profile"),
            eq("who"), eq(AiAutoMemoryType.USER), eq("body"));
    }

    @Test
    void testResolverThrowsWhenContextMissing() {
        DbMemoryResourceResolver resolver = new DbMemoryResourceResolver(aiAutoMemoryService);

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> resolver.resolve("note.md", new ToolContext(Map.of())))
            .isInstanceOf(IllegalStateException.class);
    }
}
