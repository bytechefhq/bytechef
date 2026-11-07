/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Table("component_rule")
public class ComponentRule {

    public enum RulePhase {
        BEFORE, AFTER
    }

    public enum RuleAction {
        BLOCK, TAG, REQUIRE_APPROVAL
    }

    @Id
    private Long id;

    @Column("component_name")
    private String componentName;

    @Column("tool_name")
    private @Nullable String toolName;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    @Column("phase")
    private int phase;

    @Column("rule_action")
    private int ruleAction;

    @Column("condition")
    private String condition;

    @Column("description")
    private @Nullable String description;

    @Column("enabled")
    private boolean enabled = true;

    @Column("strict")
    private boolean strict;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public ComponentRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public @Nullable String getToolName() {
        return toolName;
    }

    public void setToolName(@Nullable String toolName) {
        this.toolName = toolName;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean appliesToWorkspace(@Nullable Long workspaceId) {
        return this.workspaceId == null || this.workspaceId.equals(workspaceId);
    }

    public RulePhase getPhase() {
        return RulePhase.values()[phase];
    }

    public void setPhase(RulePhase phase) {
        this.phase = phase.ordinal();
    }

    public RuleAction getRuleAction() {
        return RuleAction.values()[ruleAction];
    }

    public void setRuleAction(RuleAction ruleAction) {
        this.ruleAction = ruleAction.ordinal();
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public int getVersion() {
        return version;
    }

    public boolean appliesToTool(String toolName) {
        return this.toolName == null || this.toolName.equals(toolName);
    }
}
