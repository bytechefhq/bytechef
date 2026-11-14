/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationCodeWorkflowFacade {

    Integration createEmptyCodeWorkflow(String componentName, Language language);

    Integration createEmptyCodeWorkflow(
        String componentName, Language language, @Nullable String name, @Nullable String description,
        @Nullable Long categoryId, @Nullable List<String> tags, @Nullable String permissionExpression);

    List<Integration> getCodeWorkflowIntegrations();

    Optional<String> getCodeWorkflowLanguage(long integrationId);

    String getCodeWorkflowSource(long integrationId);

    void save(byte[] bytes, Language language);

    /**
     * Publishes the integration like {@link #save(byte[], Language)} and, when {@code enable} is set, makes the
     * published version live in {@code environment}.
     */
    void save(byte[] bytes, Language language, Environment environment, boolean enable);

    void updateCodeWorkflowSource(long integrationId, String content);
}
