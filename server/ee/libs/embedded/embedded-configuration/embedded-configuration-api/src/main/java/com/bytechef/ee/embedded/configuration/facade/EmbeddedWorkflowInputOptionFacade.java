/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.component.domain.Option;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedWorkflowInputOptionFacade {

    List<Option> getWorkflowInputOptions(
        long integrationInstanceId, String workflowUuid, String inputName, String propertyName,
        Map<String, ?> lookupDependsOnValues, String searchText);
}
