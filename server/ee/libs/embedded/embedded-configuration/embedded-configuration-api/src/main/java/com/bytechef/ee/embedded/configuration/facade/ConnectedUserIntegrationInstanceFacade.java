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
public interface ConnectedUserIntegrationInstanceFacade {

    void disableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid);

    void enableIntegrationInstanceWorkflow(String externalUserId, long id, String workflowUuid);

    List<Option> getComponentInputOptions(
        String externalUserId, long id, String componentName, int componentVersion, String groupName,
        String propertyName, Map<String, Object> lookupDependsOnValues, String searchText);

    void updateIntegrationInstanceWorkflow(
        String externalUserId, long id, String workflowUuid, Map<String, Object> inputs);
}
