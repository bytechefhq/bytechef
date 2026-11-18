/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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
                            perform: (input, context) => 'hello from js'
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
                                    .perform((input, context) -> "hello from java")));
            }
        }
        """;

    private static final String CONTEXT_JAVA_SOURCE = """
        import com.bytechef.automation.project.ProjectHandler;
        import com.bytechef.automation.project.definition.ProjectDefinition;
        import com.bytechef.automation.project.definition.ProjectDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;
        import java.util.Map;

        public class ContextLoaderTestProjectHandler implements ProjectHandler {

            @Override
            public ProjectDefinition getDefinition() {
                return ProjectDsl.project("context-loader-test-project")
                    .version("2.0.0")
                    .workflows(
                        WorkflowDsl.workflow("loader-workflow")
                            .tasks(
                                WorkflowDsl.task("loader-task")
                                    .perform((input, context) -> context.component(
                                        "mock", "doIt", Map.of("x", 1), "conn"))));
            }
        }
        """;

    private static final String LAZY_HELPER_JAVA_SOURCE = """
        import com.bytechef.automation.project.ProjectHandler;
        import com.bytechef.automation.project.definition.ProjectDefinition;
        import com.bytechef.automation.project.definition.ProjectDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;

        public class LazyHelperProjectHandler implements ProjectHandler {

            @Override
            public ProjectDefinition getDefinition() {
                return ProjectDsl.project("lazy-helper-project")
                    .version("1.0.0")
                    .workflows(
                        WorkflowDsl.workflow("lazy-workflow")
                            .tasks(
                                WorkflowDsl.task("lazy-task")
                                    .perform((input, context) -> LazyHelper.greet("ByteChef"))));
            }
        }

        class LazyHelper {

            static String greet(String name) {
                return "Hello " + name;
            }
        }
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testLoadProjectHandlerFromJavaJarWithClassLoaderThreadsTaskContext() throws Exception {
        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, CONTEXT_JAVA_SOURCE, "ContextLoaderTestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, "context-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

        RecordingTaskContext taskContext = new RecordingTaskContext("context result");

        Object result = perform(taskDefinition, Map.of(), Map.of(), taskContext);

        assertEquals("context result", result);
        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals(Map.of("x", 1), taskContext.getInput());
        assertEquals("conn", taskContext.getConnectionName());
    }

    @Test
    void testLoadProjectHandlerFromJavaScript() throws Exception {
        Path scriptPath = tempDir.resolve("project.js");

        Files.writeString(scriptPath, JAVASCRIPT_SOURCE);

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(scriptPath), Language.JAVASCRIPT, "js-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("test-project", projectDefinition.getName());
        assertEquals("1.2.3", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        assertEquals(1, workflows.size());

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

        assertEquals("hello from js", perform(taskDefinition, Map.of(), Map.of(), new RecordingTaskContext(null)));
    }

    @Test
    void testLoadProjectHandlerFromJavaJarWithClassLoader() throws Exception {
        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, JAVA_SOURCE, "LoaderTestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, "class-loader-cache-key",
            new ConcurrentMapCacheManager());

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("loader-test-project", projectDefinition.getName());
        assertEquals("2.0.0", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

        assertEquals("hello from java", perform(taskDefinition, Map.of(), Map.of(), new RecordingTaskContext(null)));
    }

    @Test
    void testLoadProjectHandlerKeepsJarReadableForClassesFirstUsedAtPerform() throws Exception {
        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, LAZY_HELPER_JAVA_SOURCE, "LazyHelperProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, "lazy-helper-cache-key", new ConcurrentMapCacheManager());

        assertEquals("Hello ByteChef", performFirstTask(projectHandler));
    }

    @Test
    void testLoadProjectHandlerTwiceReturnsCachedHandlerThatStillPerforms() throws Exception {
        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, LAZY_HELPER_JAVA_SOURCE, "LazyHelperProjectHandler");
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();

        ProjectHandler firstProjectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, "lazy-helper-cache-key", cacheManager);
        ProjectHandler secondProjectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(jarPath), Language.JAVA, "lazy-helper-cache-key", cacheManager);

        assertSame(firstProjectHandler, secondProjectHandler);
        assertEquals("Hello ByteChef", performFirstTask(secondProjectHandler));
    }

    private static Object performFirstTask(ProjectHandler projectHandler) throws Exception {
        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

        return perform(taskDefinition, Map.of(), Map.of(), new RecordingTaskContext(null));
    }

    private static URL toUrl(Path path) throws MalformedURLException {
        URI uri = path.toUri();

        return uri.toURL();
    }

    private static Object perform(
        TaskDefinition taskDefinition, Map<String, ?> input, Map<String, ?> connection, TaskContext context)
        throws Exception {

        PerformFunction performFunction = taskDefinition.getPerform();

        return performFunction.apply(input, connection, context);
    }
}
