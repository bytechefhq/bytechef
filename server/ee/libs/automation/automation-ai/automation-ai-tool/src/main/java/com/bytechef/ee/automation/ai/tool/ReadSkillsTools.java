/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool;

import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Read-only skill tools implementation that delegates to SkillsTools.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ReadSkillsTools {

    private final SkillsTools delegate;

    @SuppressFBWarnings("EI")
    public ReadSkillsTools(SkillsTools skillsTools) {
        this.delegate = skillsTools;
    }

    @Tool(description = "Get an AI skill by its ID. Returns the skill metadata.")
    public AiSkill getAiSkill(
        @ToolParam(description = "The ID of the skill to retrieve") long id) {
        return delegate.getAiSkill(id);
    }

    @Tool(description = "Read the content of a file inside an AI skill archive. Returns the file content as text.")
    public String getAiSkillFileContent(
        @ToolParam(description = "The ID of the skill") long id,
        @ToolParam(description = "The path of the file within the skill archive") String path) {
        return delegate.getAiSkillFileContent(id, path);
    }

    @Tool(description = "List all file paths contained in an AI skill archive.")
    public List<String> getAiSkillFilePaths(
        @ToolParam(description = "The ID of the skill") long id) {
        return delegate.getAiSkillFilePaths(id);
    }

    @Tool(description = "List all available AI skills. Returns a list of skill metadata.")
    public List<AiSkill> getAiSkills() {
        return delegate.getAiSkills();
    }
}
