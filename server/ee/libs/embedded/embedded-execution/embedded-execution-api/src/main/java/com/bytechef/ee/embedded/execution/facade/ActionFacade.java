/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ActionFacade {

    Object executeAction(
        String externalUserId, String componentName, Integer componentVersion, String actionName,
        Map<String, Object> inputParameters, @Nullable Long instanceId, Environment environment);
}
