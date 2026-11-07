/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface ComponentRuleSettingsService {

    ComponentRuleSettings getSettings(@Nullable Long workspaceId);

    Optional<ComponentRuleSettings> fetchWorkspaceOverride(@Nullable Long workspaceId);

    ComponentRuleSettings saveSettings(ComponentRuleSettings componentRuleSettings, @Nullable Long workspaceId);
}
