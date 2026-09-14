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

    private static final List<String> EXCLUDED_COMPONENT_NAMES = List.of(
        "apiPlatform", "codeWorkflow", "dataTable", "knowledgeBase", "webhook");

    /**
     * Returns information if componentDefinition should be retained or skipped within the context of how the enterprise
     * subscription is integrated within the customer's software ecosystem.
     *
     * @param componentDefinition the component definition
     * @return true if component definition is allowed in this type of platform usage, otherwise false
     */
    @Override
    public boolean filter(ComponentDefinition componentDefinition) {
        return !EXCLUDED_COMPONENT_NAMES.contains(componentDefinition.getName());
    }

    @Override
    public boolean supports(PlatformType type) {
        return PlatformType.EMBEDDED.equals(type);
    }
}
