/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.ee.platform.codeworkflow.loader.AbstractHandlerPolyglotEngine;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.graalvm.polyglot.Value;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerPolyglotEngine extends AbstractHandlerPolyglotEngine<IntegrationHandler> {

    private static final IntegrationHandlerPolyglotEngine INSTANCE = new IntegrationHandlerPolyglotEngine();

    static IntegrationHandler load(String languageId, String script) {
        return INSTANCE.loadHandler(languageId, script);
    }

    @Override
    protected IntegrationHandler createHandler(Value value, List<WorkflowDefinition> workflows) {
        String componentName = Objects.requireNonNull(getMember(value, "componentName"));

        Value componentVersionValue = getValue(value, "componentVersion");

        int componentVersion = componentVersionValue.asInt();

        String description = getMember(value, "description");
        String name = getMember(value, "name");
        String version = getMember(value, "version");

        return () -> new PolyglotIntegrationDefinition(
            componentName, componentVersion, description, name, version, workflows);
    }

    private record PolyglotIntegrationDefinition(
        String componentName, int componentVersion, String description, String name, String version,
        List<WorkflowDefinition> workflows)
        implements IntegrationDefinition {

        @Override
        public Optional<String> getCategory() {
            return Optional.empty();
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getName() {
            return Optional.ofNullable(name);
        }

        @Override
        public boolean isMultipleInstances() {
            return false;
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }

        @Override
        public String getVersion() {
            return version == null ? "0.0.1" : version;
        }

        @Override
        public Optional<List<WorkflowDefinition>> getWorkflows() {
            return Optional.ofNullable(workflows);
        }
    }
}
