/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.component.rule.ComponentRule;
import com.bytechef.ee.platform.component.rule.ComponentRule.RuleAction;
import com.bytechef.ee.platform.component.rule.ComponentRule.RulePhase;
import com.bytechef.ee.platform.component.rule.config.ComponentRuleIntTestConfiguration;
import com.bytechef.ee.platform.component.rule.service.ComponentRuleServiceImpl;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ComponentRuleIntTestConfiguration.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
public class ComponentRuleRepositoryIntTest {

    @Autowired
    private ComponentRuleRepository componentRuleRepository;

    @Autowired
    private ComponentRuleServiceImpl componentRuleService;

    @AfterEach
    public void afterEach() {
        componentRuleRepository.deleteAll();
    }

    @Test
    void testInsertRoundTripsEveryColumn() {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setComponentName("slack");
        componentRule.setToolName("sendMessage");
        componentRule.setPhase(RulePhase.AFTER);
        componentRule.setRuleAction(RuleAction.TAG);
        componentRule.setCondition("output['ok'] == false");
        componentRule.setDescription("Flag failed sends");
        componentRule.setEnabled(true);
        componentRule.setStrict(true);

        ComponentRule savedComponentRule = componentRuleRepository.save(componentRule);

        assertThat(savedComponentRule.getId()).isNotNull();

        ComponentRule reloadedComponentRule = componentRuleRepository.findById(savedComponentRule.getId())
            .orElseThrow();

        assertThat(reloadedComponentRule.getComponentName()).isEqualTo("slack");
        assertThat(reloadedComponentRule.getToolName()).isEqualTo("sendMessage");
        assertThat(reloadedComponentRule.getPhase()).isEqualTo(RulePhase.AFTER);
        assertThat(reloadedComponentRule.getRuleAction()).isEqualTo(RuleAction.TAG);
        assertThat(reloadedComponentRule.getCondition()).isEqualTo("output['ok'] == false");
        assertThat(reloadedComponentRule.getDescription()).isEqualTo("Flag failed sends");
        assertThat(reloadedComponentRule.isEnabled()).isTrue();
        assertThat(reloadedComponentRule.isStrict()).isTrue();
        assertThat(reloadedComponentRule.getId()).isEqualTo(savedComponentRule.getId());
    }

    @Test
    void testSaveComponentRuleUpdatesInPlaceRatherThanInserting() {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setComponentName("slack");
        componentRule.setToolName("sendMessage");
        componentRule.setPhase(RulePhase.BEFORE);
        componentRule.setRuleAction(RuleAction.BLOCK);
        componentRule.setCondition("true");
        componentRule.setEnabled(true);

        ComponentRule savedComponentRule = componentRuleService.saveComponentRule(componentRule);
        Long id = savedComponentRule.getId();

        ComponentRule updateComponentRule = new ComponentRule();

        updateComponentRule.setId(id);
        updateComponentRule.setComponentName("slack");
        updateComponentRule.setToolName("sendMessage");
        updateComponentRule.setPhase(RulePhase.BEFORE);
        updateComponentRule.setRuleAction(RuleAction.TAG);
        updateComponentRule.setCondition("contains(inputParameters['channel'], 'C05')");
        updateComponentRule.setDescription("Now a tag rule");
        updateComponentRule.setEnabled(false);

        componentRuleService.saveComponentRule(updateComponentRule);

        List<ComponentRule> allComponentRules = componentRuleRepository.findAllByComponentName("slack");

        assertThat(allComponentRules).hasSize(1);

        ComponentRule reloadedComponentRule = componentRuleRepository.findById(id)
            .orElseThrow();

        assertThat(reloadedComponentRule.getRuleAction()).isEqualTo(RuleAction.TAG);
        assertThat(reloadedComponentRule.getCondition()).isEqualTo("contains(inputParameters['channel'], 'C05')");
        assertThat(reloadedComponentRule.getDescription()).isEqualTo("Now a tag rule");
        assertThat(reloadedComponentRule.isEnabled()).isFalse();
    }

    @Test
    void testFindAllEnabledForWorkspaceReturnsTheWorkspacesRulesAndTheTenantWideOnesOnly() {
        componentRuleRepository.save(newRule("sendMessage", null, true));
        componentRuleRepository.save(newRule("deleteMessage", 42L, true));
        componentRuleRepository.save(newRule("archiveMessage", 99L, true));

        List<ComponentRule> componentRules = componentRuleRepository.findAllEnabledForWorkspace("slack", 42L);

        assertThat(componentRules)
            .extracting(ComponentRule::getToolName)
            .containsExactlyInAnyOrder("sendMessage", "deleteMessage");
    }

    @Test
    void testFindAllEnabledForWorkspaceExcludesDisabledRows() {
        componentRuleRepository.save(newRule("sendMessage", null, true));
        componentRuleRepository.save(newRule("deleteMessage", 42L, false));

        List<ComponentRule> componentRules = componentRuleRepository.findAllEnabledForWorkspace("slack", 42L);

        assertThat(componentRules)
            .extracting(ComponentRule::getToolName)
            .containsExactly("sendMessage");
    }

    private static ComponentRule newRule(String toolName, Long workspaceId, boolean enabled) {
        ComponentRule componentRule = new ComponentRule();

        componentRule.setComponentName("slack");
        componentRule.setToolName(toolName);
        componentRule.setPhase(RulePhase.BEFORE);
        componentRule.setRuleAction(RuleAction.TAG);
        componentRule.setCondition("true");
        componentRule.setEnabled(enabled);
        componentRule.setWorkspaceId(workspaceId);

        return componentRule;
    }
}
