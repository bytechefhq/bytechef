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

package com.bytechef.automation.ai.agent.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent definition of an AiAgent — a project-scoped, LLM-backed automation entity with a set of channels (inbound
 * triggers) and elements (tools, sub-agents, and other building blocks) attached to it.
 *
 * <p>
 * The slugified {@link #name} is the stable identifier; {@link #title} is the display label that can change without
 * invalidating references. Same shape as {@code AiHubTask} for the same reasons.
 * </p>
 *
 * @author Ivica Cardic
 */
@Table("ai_agent")
public final class AiAgent {

    /**
     * Slugified-name format. Lowercase letters, digits, hyphens, and underscores; non-empty; max 64 chars. The matching
     * {@code ck_agent_name_slug} CHECK constraint is in the Liquibase init migration so any non-service write path is
     * rejected at the DB.
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    /** Hard upper bound on display title length, matching the {@code VARCHAR(255)} column. */
    public static final int MAX_TITLE_LENGTH = 255;

    /** Hard upper bound on description length, matching the {@code VARCHAR(1024)} column. */
    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("title")
    private String title;

    @Column("description")
    private @Nullable String description;

    @Column("instructions")
    private @Nullable String instructions;

    /**
     * Workspace the agent belongs to. Nullable: an agent belongs to at most one workspace, and null means none applies.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    @Column("project_id")
    private long projectId;

    @Column("uuid")
    private UUID uuid;

    /**
     * Per-agent settings — currently just {@code builtInTools}, controlling which {@code aiAgentUtils} built-in tools
     * {@code AiAgentWorkflowGenerator} emits ({@code askUserQuestion}, {@code autoMemory}, {@code skills},
     * {@code skillManagement}, {@code webSearch} plus its {@code webSearchConnectionId}) — see
     * {@code com.bytechef.automation.ai.agent.util.AiAgentSettings} for the key names and each built-in's default.
     * Absence of a key (including a wholly {@code null}/empty {@code settings} column) means that built-in's documented
     * default applies; there is no separate "unset" state to distinguish from "default".
     */
    @Column
    private MapWrapper settings = new MapWrapper();

    @MappedCollection(idColumn = "ai_agent_id")
    private Set<AiAgentTag> aiAgentTags = new HashSet<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public AiAgent() {
    }

    public AiAgent(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AiAgent agent = (AiAgent) o;

        return Objects.equals(id, agent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("AiAgent.name must not be null");
        }

        if (!NAME_PATTERN.matcher(name)
            .matches()) {
            throw new IllegalArgumentException(
                "AiAgent.name must match " + NAME_PATTERN.pattern() + " — got '" + name + "'");
        }

        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "AiAgent.title must be at most " + MAX_TITLE_LENGTH + " characters (got " + title.length() + ")");
        }

        this.title = title;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "AiAgent.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters (got "
                    + description.length() + ")");
        }

        this.description = description;
    }

    public @Nullable String getInstructions() {
        return instructions;
    }

    public void setInstructions(@Nullable String instructions) {
        this.instructions = instructions;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Map<String, ?> getSettings() {
        return settings.getMap();
    }

    public void setSettings(Map<String, ?> settings) {
        this.settings = new MapWrapper(settings);
    }

    public List<Long> getTagIds() {
        return aiAgentTags
            .stream()
            .map(AiAgentTag::getTagId)
            .toList();
    }

    public void setTagIds(List<Long> tagIds) {
        this.aiAgentTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (long tagId : tagIds) {
                aiAgentTags.add(new AiAgentTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            setTagIds(List.of());
        } else {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "AiAgent{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", workspaceId=" + workspaceId +
            ", projectId=" + projectId +
            ", uuid=" + uuid +
            ", version=" + version +
            '}';
    }
}
