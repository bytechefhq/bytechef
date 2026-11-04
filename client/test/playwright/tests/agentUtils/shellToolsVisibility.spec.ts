import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {apiJson} from '../../utils/apiUtils';
import {getProjectWorkflows, importWorkflowDefinition} from '../../utils/deploymentUtils';

export const test = mergeTests(loginTest(), projectTest);

const isShellEnabled = process.env.BYTECHEF_AGENT_UTILS_SHELL_ENABLED === 'true';

const AGENT_WORKFLOW = {
    description: 'Agent utils tools',
    label: 'Agent utils tools',
    tasks: [
        {
            clusterElements: {
                model: {name: 'model_1', parameters: {model: 'gpt-4o-mini'}, type: 'openAi/v1/model'},
                tools: [],
            },
            label: 'AI Agent',
            name: 'aiAgent_1',
            parameters: {userPrompt: 'Hello'},
            type: 'aiAgent/v1/chat',
        },
    ],
    triggers: [{name: 'trigger_1', parameters: {}, type: 'manual/v1/manual'}],
};

interface ComponentDefinitionI {
    clusterElements?: Array<{name: string}>;
}

test.describe('AI Agent Utils Shell Tools', () => {
    test(`Shell Tools are ${isShellEnabled ? 'offered' : 'hidden'} when shell.enabled is ${isShellEnabled ? 'on' : 'off'}`, async ({
        authenticatedPage,
        project,
    }) => {
        await test.step('The component definition matches the server setting', async () => {
            const componentDefinition = await apiJson<ComponentDefinitionI>({
                method: 'GET',
                page: authenticatedPage,
                path: '/api/platform/internal/component-definitions/aiAgentUtils/versions/1',
            });

            const clusterElementNames = (componentDefinition.clusterElements ?? []).map(
                (clusterElement) => clusterElement.name
            );

            expect(clusterElementNames).toContain('fileSystemTools');

            if (isShellEnabled) {
                expect(clusterElementNames).toContain('shellTools');
            } else {
                expect(clusterElementNames).not.toContain('shellTools');
            }
        });

        await test.step('Open the AI Agent Utils tools in the agent Tools slot', async () => {
            await importWorkflowDefinition(authenticatedPage, project.id, AGENT_WORKFLOW);

            const [projectWorkflow] = await getProjectWorkflows(authenticatedPage, project.id);

            const workflowPage = new WorkflowPage(authenticatedPage);

            await workflowPage.goToWorkflowEditor(project.id, String(projectWorkflow.projectWorkflowId));

            await authenticatedPage.locator('.react-flow__node[data-id="aiAgent_1-tools-placeholder-0"]').click();

            const componentsDialog = authenticatedPage.getByRole('dialog');

            await componentsDialog.getByRole('textbox', {name: 'Filter components'}).fill('AI Agent Utils');

            await componentsDialog.getByText('AI Agent Utils', {exact: true}).click();

            await expect(componentsDialog.getByRole('heading', {exact: true, name: 'File System Tools'})).toBeVisible();
        });

        await test.step(`Shell Tools are ${isShellEnabled ? 'listed' : 'not listed'}`, async () => {
            const shellToolsHeading = authenticatedPage
                .getByRole('dialog')
                .getByRole('heading', {exact: true, name: 'Shell Tools'});

            if (isShellEnabled) {
                await expect(shellToolsHeading).toBeVisible();
            } else {
                await expect(shellToolsHeading).toHaveCount(0);
            }
        });
    });
});
