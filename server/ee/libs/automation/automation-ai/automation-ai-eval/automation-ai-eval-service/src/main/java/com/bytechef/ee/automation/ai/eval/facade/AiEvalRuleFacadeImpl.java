/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.service.AiEvalExecutionService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalRuleService;
import com.bytechef.ee.platform.ai.eval.service.AiEvalScoreConfigService;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplate;
import com.bytechef.ee.platform.ai.eval.template.EvalTemplateCatalog;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiEvalRuleFacadeImpl implements AiEvalRuleFacade {

    private final AiEvalExecutionService aiEvalExecutionService;
    private final AiEvalRuleService aiEvalRuleService;
    private final AiEvalScoreConfigService aiEvalScoreConfigService;
    private final WorkspaceAiEvalRuleService workspaceAiEvalRuleService;

    @SuppressFBWarnings("EI")
    AiEvalRuleFacadeImpl(
        AiEvalExecutionService aiEvalExecutionService, AiEvalRuleService aiEvalRuleService,
        AiEvalScoreConfigService aiEvalScoreConfigService, WorkspaceAiEvalRuleService workspaceAiEvalRuleService) {

        this.aiEvalExecutionService = aiEvalExecutionService;
        this.aiEvalRuleService = aiEvalRuleService;
        this.aiEvalScoreConfigService = aiEvalScoreConfigService;
        this.workspaceAiEvalRuleService = workspaceAiEvalRuleService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule getEvalRule(long id) {
        return aiEvalRuleService.getEvalRule(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiEvalRule> getEvalRulesByWorkspace(Long workspaceId) {
        return workspaceAiEvalRuleService.getEvalRulesByWorkspace(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalExecution> getExecutionsByEvalRule(Long evalRuleId) {
        return aiEvalExecutionService.getExecutionsByEvalRule(evalRuleId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiEvalExecution> getExecutionsByTrace(Long traceId) {
        return aiEvalExecutionService.getExecutionsByTrace(traceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule createInWorkspace(AiEvalRule evalRule, Long workspaceId) {
        return workspaceAiEvalRuleService.createInWorkspace(evalRule, workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteInWorkspace(long id) {
        workspaceAiEvalRuleService.deleteInWorkspace(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule update(AiEvalRule evalRule) {
        return aiEvalRuleService.update(evalRule);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<EvalTemplate> listTemplates() {
        return EvalTemplateCatalog.templates();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiEvalRule instantiateTemplate(
        String templateKey, Long workspaceId, Long projectId, String model, BigDecimal samplingRate) {

        EvalTemplate template = EvalTemplateCatalog.byKey(templateKey)
            .orElseThrow(() -> new IllegalArgumentException("Unknown eval template: " + templateKey));

        AiEvalScoreConfig scoreConfig = new AiEvalScoreConfig(template.title());

        scoreConfig.setDataType(template.dataType());
        scoreConfig.setDescription(template.description());
        scoreConfig.setMinValue(template.minValue());
        scoreConfig.setMaxValue(template.maxValue());

        AiEvalScoreConfig savedScoreConfig = aiEvalScoreConfigService.create(scoreConfig);

        AiEvalRule evalRule = new AiEvalRule(
            template.title(), savedScoreConfig.getId(), template.promptTemplate(), model, samplingRate);

        evalRule.setProjectId(projectId);

        return workspaceAiEvalRuleService.createInWorkspace(evalRule, workspaceId);
    }
}
