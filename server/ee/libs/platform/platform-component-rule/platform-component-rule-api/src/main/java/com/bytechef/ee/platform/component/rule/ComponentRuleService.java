/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface ComponentRuleService {

    List<ComponentRule> getComponentRules();

    List<ComponentRule> getComponentRules(String componentName);

    ComponentRule getComponentRule(long id);

    List<ComponentRule> getEnabledComponentRules(String componentName, @Nullable Long workspaceId);

    ComponentRule saveComponentRule(ComponentRule componentRule);

    void deleteComponentRule(long id);
}
