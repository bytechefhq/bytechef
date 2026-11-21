/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.loader.AbstractHandlerPolyglotEngine;
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
class ProjectHandlerPolyglotEngine extends AbstractHandlerPolyglotEngine<ProjectHandler> {

    private static final ProjectHandlerPolyglotEngine INSTANCE = new ProjectHandlerPolyglotEngine();

    static ProjectHandler load(String languageId, String script) {
        return INSTANCE.loadHandler(languageId, script);
    }

    @Override
    protected ProjectHandler createHandler(Value value, List<WorkflowDefinition> workflows) {
        String name = Objects.requireNonNull(getMember(value, "name"));
        String description = getMember(value, "description");
        String version = getMember(value, "version");

        return () -> new PolyglotProjectDefinition(name, description, version, workflows);
    }

    private record PolyglotProjectDefinition(
        String name, String description, String version, List<WorkflowDefinition> workflows)
        implements ProjectDefinition {

        @Override
        public Optional<String> getCategory() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version == null ? "0.0.1" : version;
        }

        @Override
        public List<WorkflowDefinition> getWorkflows() {
            return List.copyOf(workflows);
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }
    }
}
