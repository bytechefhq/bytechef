/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record ComponentRuleSettings(boolean observeMode, int approvalExpiresInHours) {

    public static final String PROPERTY_KEY = "component_rule_settings";

    public static final ComponentRuleSettings DEFAULT = new ComponentRuleSettings(false, 1440);
}
