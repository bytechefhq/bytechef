/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserComponentInputFacade {

    List<Option> getComponentInputOptions(
        String externalUserId, Environment environment, String componentName, int componentVersion, String groupName,
        String propertyName, Map<String, Object> lookupDependsOnValues, @Nullable String searchText, long connectionId);
}
