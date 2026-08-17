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

import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Enforces per-user owner-isolation on the AI skill HTTP entry points. Every by-id operation is authorized against the
 * skill's {@code createdBy} login (tenant/system admins bypass) before delegating to the shared {@link AiSkillFacade};
 * {@link #getAiSkills()} is filtered to the caller's own skills. The runtime AI agent tools keep calling
 * {@link AiSkillFacade} directly and are unaffected.
 *
 * @author Ivica Cardic
 */
@Service
class AiSkillApiFacadeImpl implements AiSkillApiFacade {

    private final AiSkillFacade aiSkillFacade;

    AiSkillApiFacadeImpl(AiSkillFacade aiSkillFacade) {
        this.aiSkillFacade = aiSkillFacade;
    }

    @Override
    public AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes) {
        return aiSkillFacade.createAiSkill(name, description, filename, bytes);
    }

    @Override
    public AiSkill createAiSkillFromInstructions(String name, @Nullable String description, String instructions) {
        return aiSkillFacade.createAiSkillFromInstructions(name, description, instructions);
    }

    @Override
    public AiSkill createAdditionalFilesInSkill(long id, Map<String, String> additionalFiles) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.createAdditionalFilesInSkill(id, additionalFiles);
    }

    @Override
    public void deleteAiSkill(long id) {
        checkOwnerOrAdmin(id);

        aiSkillFacade.deleteAiSkill(id);
    }

    @Override
    public AiSkill getAiSkill(long id) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.getAiSkill(id);
    }

    @Override
    public AiSkillFacade.AiSkillDownload getAiSkillWithDownload(long id) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.getAiSkillWithDownload(id);
    }

    @Override
    public String getAiSkillFileContent(long id, String path) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.getAiSkillFileContent(id, path);
    }

    @Override
    public List<String> getAiSkillFilePaths(long id) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.getAiSkillFilePaths(id);
    }

    @Override
    public List<AiSkill> getAiSkills() {
        if (isAdmin()) {
            return aiSkillFacade.getAiSkills();
        }

        String currentUserLogin = SecurityUtils.fetchCurrentUserLogin()
            .orElse(null);

        if (currentUserLogin == null) {
            return List.of();
        }

        return aiSkillFacade.getAiSkills()
            .stream()
            .filter(aiSkill -> currentUserLogin.equals(aiSkill.getCreatedBy()))
            .toList();
    }

    @Override
    public List<Tag> getAiSkillTags() {
        return aiSkillFacade.getTags(
            getAiSkills()
                .stream()
                .flatMap(aiSkill -> aiSkill.getTagIds()
                    .stream())
                .distinct()
                .toList());
    }

    @Override
    public List<Tag> getTags(List<Long> tagIds) {
        return aiSkillFacade.getTags(tagIds);
    }

    @Override
    public AiSkill removeFileInSkill(long id, String path) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.removeFileInSkill(id, path);
    }

    @Override
    public AiSkill updateAiSkill(long id, String name, @Nullable String description) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.updateAiSkill(id, name, description);
    }

    @Override
    public AiSkill updateAiSkillTags(long id, List<Tag> tags) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.updateAiSkillTags(id, tags);
    }

    @Override
    public AiSkill updateAiSkillContent(long id, @Nullable String path, String content) {
        checkOwnerOrAdmin(id);

        return aiSkillFacade.updateAiSkillContent(id, path, content);
    }

    /**
     * Authorizes the current user against the skill's owner. Tenant/system admins bypass; otherwise the caller's login
     * must equal the skill's {@code createdBy}. Fails closed (throws {@code AccessDeniedException}) when there is no
     * authenticated user or the skill has no recorded owner.
     */
    private void checkOwnerOrAdmin(long id) {
        if (isAdmin()) {
            return;
        }

        AiSkill aiSkill = aiSkillFacade.getAiSkill(id);

        SecurityUtils.checkCurrentUserLogin(aiSkill.getCreatedBy());
    }

    private boolean isAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }
}
