/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface PropertyCopilotFacade {

    PropertyCopilotResult generatePropertyValue(PropertyCopilotRequest request);
}
