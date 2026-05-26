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

package com.bytechef.ai.mcp.tool.automation;

import com.bytechef.ai.mcp.tool.automation.exception.SkillToolErrorType;
import com.bytechef.ee.platform.ai.skill.domain.AiSkill;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component
public class SkillsTools {
    private static final Logger log = LoggerFactory.getLogger(SkillsTools.class);

    private final AiSkillFacade aiSkillFacade;

    @SuppressFBWarnings("EI")
    public SkillsTools(AiSkillFacade aiSkillFacade) {
        this.aiSkillFacade = aiSkillFacade;
    }

    @Tool(
        description = "Create a new AI skill from text instructions. The skill metadata must follow the " +
            "agentskills.io SKILL.md spec — see parameter descriptions for the format rules. Returns the created skill.")
    public AiSkill createAiSkill(
        @ToolParam(
            description = "The skill name. 1-64 characters; letters (a-z, A-Z), digits (0-9), with single " +
                "hyphens (-) or single spaces as separators. Must not start or end with a separator and must " +
                "not contain consecutive separators. Example: 'pdf-processing'.") String name,
        @ToolParam(
            description = "A description (max 1024 characters) of what the skill does and when to use it. " +
                "Include keywords that help identify relevant tasks.") @Nullable String description,
        @ToolParam(
            description = "The Markdown body of SKILL.md — the actual skill instructions. Do NOT include the " +
                "YAML frontmatter (the --- block); it is generated automatically from the name and description.") String instructions,
        @ToolParam(
            required = false,
            description = "Optional bundled files to include in the skill archive alongside SKILL.md. " +
                "Keys are relative paths within the archive (e.g. 'scripts/extract.py', " +
                "'references/REFERENCE.md', 'assets/template.txt'). Values are the file contents as text. " +
                "Paths must not be absolute or contain traversal sequences (..).") @Nullable Map<String, String> additionalFiles) {

        try {
            AiSkill aiSkill = aiSkillFacade.createAiSkillFromInstructions(name, description, instructions, additionalFiles);

            if (log.isDebugEnabled()) {
                log.debug("createAiSkill({}): Created skill with id={}", name, aiSkill.getId());
            }

            return aiSkill;
        } catch (Exception e) {
            log.error("createAiSkill({}): Failed to create skill", name, e);

            throw new ExecutionException(
                "Failed to create skill: " + e.getMessage(), e, SkillToolErrorType.CREATE_SKILL);
        }
    }

    @Tool(
        description = "Add or replace multiple files inside an existing AI skill archive in one operation. " +
            "Use this to bundle scripts, references, or assets into a skill after it has been created. " +
            "Returns the updated skill.")
    public AiSkill createAdditionalFilesInSkill(
        @ToolParam(description = "The ID of the skill to add files to") long id,
        @ToolParam(
            description = "Files to add or replace. Keys are relative paths within the archive " +
                "(e.g. 'scripts/extract.py', 'references/REFERENCE.md', 'assets/template.txt'). " +
                "Values are the file contents as text. Paths must not be absolute or contain " +
                "traversal sequences (..).") Map<String, String> additionalFiles) {

        try {
            AiSkill aiSkill = aiSkillFacade.createAdditionalFilesInSkill(id, additionalFiles);

            if (log.isDebugEnabled()) {
                log.debug("createAdditionalFilesInSkill({}): Added {} file(s)", id, additionalFiles.size());
            }

            return aiSkill;
        } catch (Exception e) {
            log.error("createAdditionalFilesInSkill({}): Failed to add files to skill", id, e);

            throw new ExecutionException(
                "Failed to add files to skill: " + e.getMessage(), e, SkillToolErrorType.CREATE_SKILL);
        }
    }

//    @Tool(description = "Delete an AI skill by its ID.")
    public void deleteAiSkill(
        @ToolParam(description = "The ID of the skill to delete") long id) {

        try {
            aiSkillFacade.deleteAiSkill(id);

            if (log.isDebugEnabled()) {
                log.debug("deleteAiSkill({}): Deleted skill", id);
            }
        } catch (Exception e) {
            log.error("deleteAiSkill({}): Failed to delete skill", id, e);

            throw new ExecutionException(
                "Failed to delete skill: " + e.getMessage(), e, SkillToolErrorType.DELETE_SKILL);
        }
    }

    @Tool(description = "Get an AI skill by its ID. Returns the skill metadata.")
    public AiSkill getAiSkill(
        @ToolParam(description = "The ID of the skill to retrieve") long id) {

        try {
            AiSkill aiSkill = aiSkillFacade.getAiSkill(id);

            if (log.isDebugEnabled()) {
                log.debug("getAiSkill({}): Retrieved skill '{}'", id, aiSkill.getName());
            }

            return aiSkill;
        } catch (Exception e) {
            log.error("getAiSkill({}): Failed to get skill", id, e);

            throw new ExecutionException(
                "Failed to get skill: " + e.getMessage(), e, SkillToolErrorType.GET_SKILL);
        }
    }

    @Tool(description = "Read the content of a file inside an AI skill archive. Returns the file content as text.")
    public String getAiSkillFileContent(
        @ToolParam(description = "The ID of the skill") long id,
        @ToolParam(description = "The path of the file within the skill archive") String path) {

        try {
            String content = aiSkillFacade.getAiSkillFileContent(id, path);

            if (log.isDebugEnabled()) {
                log.debug("getAiSkillFileContent({}, {}): Retrieved file content", id, path);
            }

            return content;
        } catch (Exception e) {
            log.error("getAiSkillFileContent({}, {}): Failed to read skill file content", id, path, e);

            throw new ExecutionException(
                "Failed to get skill file content: " + e.getMessage(), e, SkillToolErrorType.GET_SKILL_FILE_CONTENT);
        }
    }

    @Tool(description = "List all file paths contained in an AI skill archive.")
    public List<String> getAiSkillFilePaths(
        @ToolParam(description = "The ID of the skill") long id) {

        try {
            List<String> paths = aiSkillFacade.getAiSkillFilePaths(id);

            if (log.isDebugEnabled()) {
                log.debug("getAiSkillFilePaths({}): Found {} file(s)", id, paths.size());
            }

            return paths;
        } catch (Exception e) {
            log.error("getAiSkillFilePaths({}): Failed to list skill file paths", id, e);

            throw new ExecutionException(
                "Failed to get skill file paths: " + e.getMessage(), e, SkillToolErrorType.GET_SKILL_FILE_PATHS);
        }
    }

    @Tool(description = "List all available AI skills. Returns a list of skill metadata.")
    public List<AiSkill> getAiSkills() {
        try {
            List<AiSkill> aiSkills = aiSkillFacade.getAiSkills();

            if (log.isDebugEnabled()) {
                log.debug("getAiSkills(): Found {} skill(s)", aiSkills.size());
            }

            return aiSkills;
        } catch (Exception e) {
            log.error("getAiSkills(): Failed to list skills", e);

            throw new ExecutionException(
                "Failed to list skills: " + e.getMessage(), e, SkillToolErrorType.LIST_SKILLS);
        }
    }

    @Tool(
        description = "Update the instructions (e.g. `SKILL.md` content) of an existing AI skill. Returns the updated skill.")
    public AiSkill updateAiSkillContent(
        @ToolParam(description = "The ID of the skill to update") long id,
        @ToolParam(description = "Path to the file you want to update. Default is `SKILL.md`") String targetPath,
        @ToolParam(description = "The modified content of the skill") String contents) {

        try {
            AiSkill aiSkill = aiSkillFacade.updateAiSkillContent(id, targetPath, contents);

            if (log.isDebugEnabled()) {
                log.debug("updateAiSkillContent({}): Updated skill content", id);
            }

            return aiSkill;
        } catch (Exception e) {
            log.error("updateAiSkillContent({}): Failed to update skill content", id, e);

            throw new ExecutionException(
                "Failed to update skill content: " + e.getMessage(), e, SkillToolErrorType.UPDATE_SKILL_CONTENT);
        }
    }

    @Tool(
        description = "Update the name and description of an existing AI skill. The metadata must follow the " +
            "agentskills.io SKILL.md spec. Returns the updated skill.")
    public AiSkill updateAiSkill(
        @ToolParam(description = "The ID of the skill to update") long id,
        @ToolParam(
            description = "The new skill name. 1-64 characters; letters (a-z, A-Z), digits (0-9), with single " +
                "hyphens (-) or single spaces as separators. Must not start or end with a separator and must " +
                "not contain consecutive separators.") String name,
        @ToolParam(description = "The new description (max 1024 characters).") @Nullable String description) {

        try {
            AiSkill aiSkill = aiSkillFacade.updateAiSkill(id, name, description);

            if (log.isDebugEnabled()) {
                log.debug("updateAiSkill({}): Updated skill to name='{}'", id, name);
            }

            return aiSkill;
        } catch (Exception e) {
            log.error("updateAiSkill({}): Failed to update skill", id, e);

            throw new ExecutionException(
                "Failed to update skill: " + e.getMessage(), e, SkillToolErrorType.UPDATE_SKILL);
        }
    }
}
