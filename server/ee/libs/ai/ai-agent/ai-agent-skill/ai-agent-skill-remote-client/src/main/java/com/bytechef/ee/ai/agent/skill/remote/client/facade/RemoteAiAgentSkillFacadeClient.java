/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.agent.skill.remote.client.facade;

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAiAgentSkillFacadeClient implements AiAgentSkillFacade {

    @Override
    public AiAgentSkill createAiAgentSkill(String name, String description, String filename, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiAgentSkill createAiAgentSkillFromInstructions(String name, String description, String instructions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAiAgentSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiAgentSkill getAiAgentSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getAiAgentSkillDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiAgentSkillDownload getAiAgentSkillWithDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAiAgentSkillFileContent(long id, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAiAgentSkillFilePaths(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AiAgentSkill> getAiAgentSkills() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AiAgentSkill updateAiAgentSkill(long id, String name, String description) {
        throw new UnsupportedOperationException();
    }
}
