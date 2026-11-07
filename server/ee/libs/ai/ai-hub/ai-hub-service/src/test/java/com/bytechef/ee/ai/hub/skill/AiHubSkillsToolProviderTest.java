/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.skill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSkillsToolProviderTest {

    private static final String SKILL_MARKDOWN = """
        ---
        name: research
        description: Research a topic
        ---
        Research the topic thoroughly.
        """;

    private final AiSkillFacade aiSkillFacade = mock(AiSkillFacade.class);
    private final UserService userService = mock(UserService.class);
    private final AiHubSkillsToolProvider skillsToolProvider =
        new AiHubSkillsToolProvider(aiSkillFacade, userService);

    @Test
    void testResolvesOneSkillsToolFromTheUsersOwnSkills() throws IOException {
        when(userService.getUser(10L)).thenReturn(newUser("alice"));
        when(aiSkillFacade.getAiSkills()).thenReturn(List.of(newSkill(1L, "alice"), newSkill(2L, "bob")));
        when(aiSkillFacade.getAiSkillDownload(1L)).thenReturn(zip("research/SKILL.md", SKILL_MARKDOWN));

        List<ToolCallback> toolCallbacks = skillsToolProvider.resolve(10L);

        assertThat(toolCallbacks).hasSize(1);

        verify(aiSkillFacade, never()).getAiSkillDownload(2L);
    }

    @Test
    void testResolvesNothingWhenTheUserHasNoSkills() {
        when(userService.getUser(10L)).thenReturn(newUser("alice"));
        when(aiSkillFacade.getAiSkills()).thenReturn(List.of(newSkill(2L, "bob")));

        assertThat(skillsToolProvider.resolve(10L)).isEmpty();

        verify(aiSkillFacade, never()).getAiSkillDownload(anyLong());
    }

    @Test
    void testResolvesNothingWhenTheUserCannotBeFound() {
        when(userService.getUser(10L)).thenThrow(new IllegalArgumentException("missing"));

        assertThat(skillsToolProvider.resolve(10L)).isEmpty();
    }

    @Test
    void testSkipsASkillWhoseArchiveEscapesTheExtractionDirectory() throws IOException {
        when(userService.getUser(10L)).thenReturn(newUser("alice"));
        when(aiSkillFacade.getAiSkills()).thenReturn(List.of(newSkill(1L, "alice")));
        when(aiSkillFacade.getAiSkillDownload(1L)).thenReturn(zip("../escape/SKILL.md", SKILL_MARKDOWN));

        assertThat(skillsToolProvider.resolve(10L)).isEmpty();
    }

    private static AiSkill newSkill(long id, String createdBy) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(id);
        aiSkill.setName("skill-" + id);
        aiSkill.setCreatedBy(createdBy);

        return aiSkill;
    }

    private static User newUser(String login) {
        User user = new User();

        user.setLogin(login);

        return user;
    }

    private static byte[] zip(String entryName, String content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }

        return byteArrayOutputStream.toByteArray();
    }
}
