/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.agent.skill.remote.client.facade;

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import com.bytechef.ai.agent.skill.facade.AgentSkillFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAgentSkillFacadeClient implements AgentSkillFacade {

    @Override
    public AgentSkill createAgentSkill(String name, String description, String filename, byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentSkill createAgentSkillFromInstructions(String name, String description, String instructions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAgentSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentSkill getAgentSkill(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getAgentSkillDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentSkillDownload getAgentSkillWithDownload(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAgentSkillFileContent(long id, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAgentSkillFilePaths(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AgentSkill> getAgentSkills() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentSkill updateAgentSkill(long id, String name, String description) {
        throw new UnsupportedOperationException();
    }
}
