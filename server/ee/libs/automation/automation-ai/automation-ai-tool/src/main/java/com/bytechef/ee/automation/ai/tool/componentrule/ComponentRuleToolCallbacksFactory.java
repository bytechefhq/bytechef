/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.componentrule;

import com.bytechef.ee.platform.component.rule.ComponentRuleService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class ComponentRuleToolCallbacksFactory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ComponentRuleService componentRuleService;
    private final Evaluator evaluator;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ComponentRuleToolCallbacksFactory(
        ComponentRuleService componentRuleService, ComponentDefinitionService componentDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator) {

        this.componentRuleService = componentRuleService;
        this.componentDefinitionService = componentDefinitionService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.evaluator = evaluator;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListComponentRulesToolCallback(componentRuleService));
        toolCallbacks.add(
            new DescribeComponentToolParametersToolCallback(
                componentDefinitionService, clusterElementDefinitionService));
        toolCallbacks.add(new ProposeComponentRuleConditionToolCallback(evaluator));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateComponentRuleToolCallback(componentRuleService));

        return toolCallbacks;
    }
}
