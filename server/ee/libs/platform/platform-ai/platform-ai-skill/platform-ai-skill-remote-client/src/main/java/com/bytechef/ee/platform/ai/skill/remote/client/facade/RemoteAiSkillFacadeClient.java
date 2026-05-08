/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.client.facade;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAiSkillFacadeClient implements AiSkillFacade {

    @Override
    public AiSkill createAiSkill(String name, String description, String filename, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiSkill createAiSkillFromInstructions(String name, String description, String instructions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAiSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiSkill getAiSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getAiSkillDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiSkillDownload getAiSkillWithDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAiSkillFileContent(long id, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAiSkillFilePaths(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiSkill> getAiSkills() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiSkill updateAiSkill(long id, String name, String description) {
        throw new UnsupportedOperationException();
    }
}
