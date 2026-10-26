/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.ownership;

import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class ContextStoreOwnerContributorConfiguration {

    public static final String CONTEXT_STORE_PROJECT_NAME_PREFIX = "__CONTEXT_STORE__";

    private static final String CONTEXT_STORE_PROJECT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM project context_store_project WHERE context_store_project.id = %1$s AND " +
            "LEFT(context_store_project.name, " + CONTEXT_STORE_PROJECT_NAME_PREFIX.length() + ") = '" +
            CONTEXT_STORE_PROJECT_NAME_PREFIX + "')";

    @Bean
    ProjectOwnerContributor contextStoreProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.CONTEXT_STORE, CONTEXT_STORE_PROJECT_CLAIM_CONDITION);
    }
}
