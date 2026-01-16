/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.filter;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationComponentDefinitionFilter implements ComponentDefinitionFilter {

    private static final List<String> COMPONENT_NAMES = List.of("apiPlatform", "webhook");

    @Override
    public boolean filter(ComponentDefinition componentDefinition) {
        return !COMPONENT_NAMES.contains(componentDefinition.getName());
    }

    @Override
    public boolean supports(PlatformType type) {
        return PlatformType.EMBEDDED.equals(type);
    }
}
