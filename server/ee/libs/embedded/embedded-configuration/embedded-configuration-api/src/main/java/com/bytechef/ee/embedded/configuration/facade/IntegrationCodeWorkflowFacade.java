/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
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

    /**
     * Creates a code-backed integration together with the metadata the platform owns — the ones the source cannot
     * declare — so creation is one step rather than create-then-edit. Every metadata argument is optional; a null name
     * falls back to the component name.
     */
    Integration createEmptyCodeWorkflow(
        String componentName, Language language, @Nullable String name, @Nullable String description,
        @Nullable Long categoryId, @Nullable List<String> tags, @Nullable String permissionExpression);

    List<Integration> getCodeWorkflowIntegrations();

    Optional<String> getCodeWorkflowLanguage(long integrationId);

    String getCodeWorkflowSource(long integrationId);

    void save(byte[] bytes, Language language);

    void updateCodeWorkflowSource(long integrationId, String content);
}
