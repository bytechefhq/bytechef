/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import java.util.Set;

/**
 * Tenant-wide component visibility policy operations.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ComponentPolicyService {

    boolean isEnabled(String componentName);

    Set<String> getDisabledComponentNames();

    ComponentPolicy updateComponentPolicy(String componentName, boolean enabled);
}
