/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.assetfile;

import static com.bytechef.component.assetfile.constant.AssetFileConstants.ASSET_FILE_ID;
import static com.bytechef.component.assetfile.constant.AssetFileConstants.NEW_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.assetfile.service.AssetFileSystemFacade;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class AssetFileComponentActionAuthorizationTest {

    private static final long ASSET_FILE = 5L;
    private static final long PROJECT_ID = 9L;
    private static final long WORKSPACE_ID = 3L;
    private static final String WORKFLOW_ID = "workflow-1";

    private final AssetFileSystemFacade assetFileSystemFacade = mock(AssetFileSystemFacade.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private ComponentDefinition componentDefinition;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        Project project = mock(Project.class);
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(PROJECT_ID);
        when(project.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(projectWorkflowService.getWorkflowProjectWorkflow(WORKFLOW_ID)).thenReturn(projectWorkflow);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        AssetFileComponentHandler componentHandler =
            new AssetFileComponentHandler(assetFileSystemFacade, projectService, projectWorkflowService);

        componentDefinition = componentHandler.getDefinition();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetActionReachesTheSystemFacadeWithNoPrincipal() throws Exception {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(ASSET_FILE);
        assetFile.setName("report.csv");

        when(assetFileSystemFacade.findByIdInWorkspace(ASSET_FILE, WORKSPACE_ID)).thenReturn(assetFile);

        Object result = perform("getAssetFile", Map.of(ASSET_FILE_ID, ASSET_FILE));

        assertThat(result)
            .asInstanceOf(MAP)
            .containsEntry("id", ASSET_FILE)
            .containsEntry("name", "report.csv");

        verify(assetFileSystemFacade).findByIdInWorkspace(ASSET_FILE, WORKSPACE_ID);
    }

    @Test
    void testDeleteActionReachesTheSystemFacadeWithNoPrincipal() throws Exception {
        Object result = perform("deleteAssetFile", Map.of(ASSET_FILE_ID, ASSET_FILE));

        assertThat(result)
            .asInstanceOf(MAP)
            .containsEntry("deleted", true);

        verify(assetFileSystemFacade).deleteInWorkspace(ASSET_FILE, WORKSPACE_ID);
    }

    @Test
    void testRenameActionReachesTheSystemFacadeWithNoPrincipal() throws Exception {
        AssetFile renamed = new AssetFile();

        renamed.setId(ASSET_FILE);
        renamed.setName("renamed.csv");

        when(assetFileSystemFacade.renameInWorkspace(ASSET_FILE, WORKSPACE_ID, "renamed.csv")).thenReturn(renamed);

        Object result = perform("renameAssetFile", Map.of(ASSET_FILE_ID, ASSET_FILE, NEW_NAME, "renamed.csv"));

        assertThat(result)
            .asInstanceOf(MAP)
            .containsEntry("name", "renamed.csv");

        verify(assetFileSystemFacade).renameInWorkspace(ASSET_FILE, WORKSPACE_ID, "renamed.csv");
    }

    @Test
    void testTheHandlerTakesTheSystemFacadeAndNotTheGuardedOne() {
        Constructor<?>[] constructors = AssetFileComponentHandler.class.getConstructors();

        assertThat(constructors).hasSize(1);

        assertThat(constructors[0].getParameterTypes())
            .as("component actions run with no principal, so the handler must hold the ownership-checked system "
                + "facade; AssetFileFacade resolves the current user and would deny every worker-thread invocation")
            .contains(AssetFileSystemFacade.class)
            .doesNotContain(AssetFileFacade.class);
    }

    private Object perform(String actionName, Map<String, Object> inputParameters) throws Exception {
        List<? extends ActionDefinition> actionDefinitions = componentDefinition.getActions();

        ActionDefinition actionDefinition = actionDefinitions.stream()
            .filter(action -> actionName.equals(action.getName()))
            .findFirst()
            .orElseThrow();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        Parameters parameters = MockParametersFactory.create(inputParameters);

        return performFunction.apply(parameters, MockParametersFactory.create(Map.of()), actionContext());
    }

    private ActionContext actionContext() {
        ActionContext actionContext = mock(
            ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        when(((ActionContextAware) actionContext).getWorkflowId()).thenReturn(WORKFLOW_ID);

        return actionContext;
    }
}
