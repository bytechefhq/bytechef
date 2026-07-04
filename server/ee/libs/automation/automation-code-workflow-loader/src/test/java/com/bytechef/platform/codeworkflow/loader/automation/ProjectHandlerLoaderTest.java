/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerLoaderTest {

    private static final String JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            version: '1.2.3',
            workflows: [
                {
                    name: 'my-workflow',
                    label: 'My Workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'hello from js'
                        }
                    ]
                }
            ]
        })
        """;

    private static final String JAVA_SOURCE = """
        import com.bytechef.automation.project.ProjectHandler;
        import com.bytechef.automation.project.definition.ProjectDefinition;
        import com.bytechef.automation.project.definition.ProjectDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;

        public class LoaderTestProjectHandler implements ProjectHandler {

            @Override
            public ProjectDefinition getDefinition() {
                return ProjectDsl.project("loader-test-project")
                    .version("2.0.0")
                    .workflows(
                        WorkflowDsl.workflow("loader-workflow")
                            .tasks(
                                WorkflowDsl.task("loader-task")
                                    .perform(() -> "hello from java")));
            }
        }
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testLoadProjectHandlerFromJavaScript() throws IOException {
        Path scriptPath = tempDir.resolve("project.js");

        Files.writeString(scriptPath, JAVASCRIPT_SOURCE);

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(scriptPath), Language.JAVASCRIPT, ProjectHandlerLoader.JavaLoader.CLASS_LOADER, "js-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("test-project", projectDefinition.getName());
        assertEquals("1.2.3", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        assertEquals(1, workflows.size());

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from js", taskDefinition.getPerform()
            .apply());
    }

    @Test
    void testLoadProjectHandlerFromJavaJar() throws IOException {
        ProjectHandlerPolyglotEngineTest.assumeEspressoAvailable();

        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, JAVA_SOURCE, "LoaderTestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, ProjectHandlerLoader.JavaLoader.ESPRESSO, "espresso-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("loader-test-project", projectDefinition.getName());
        assertEquals("2.0.0", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from java", taskDefinition.getPerform()
            .apply());
    }

    @Test
    void testLoadProjectHandlerFromJavaJarWithClassLoader() throws IOException {
        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, JAVA_SOURCE, "LoaderTestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, ProjectHandlerLoader.JavaLoader.CLASS_LOADER, "class-loader-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("loader-test-project", projectDefinition.getName());
        assertEquals("2.0.0", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from java", taskDefinition.getPerform()
            .apply());
    }

    private static URL toUrl(Path path) throws MalformedURLException {
        URI uri = path.toUri();

        return uri.toURL();
    }
}
