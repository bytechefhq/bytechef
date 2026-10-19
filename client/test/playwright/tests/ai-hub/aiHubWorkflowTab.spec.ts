import {type Page, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {AiHubPage} from '../../pages/aiHubPage';
import {WorkflowPage} from '../../pages/workflowPage';
import {AI_HUB_CHAT_URL, completedTurn, stubAgUiRun, textMessage, toolCall} from '../../utils/agUiStreamStub';
import {
    type HubWorkflowI,
    openWorkflowTabArguments,
    openWorkflowTabResult,
    prepareAiHub,
    readHubWorkflow,
} from '../../utils/aiHubUtils';
import {type TestWorkflowI, createWorkflow} from '../../utils/projectUtils';

export const test = mergeTests(loginTest(), aiHubChatsTest, projectTest, importWorkflowTest);

const toHubWorkflow = (page: Page, workflow: TestWorkflowI) =>
    readHubWorkflow(page, {projectId: workflow.projectId, projectWorkflowId: Number(workflow.workflowId)});

const openWorkflowTabTurn = (hubWorkflow: HubWorkflowI) =>
    completedTurn(
        toolCall('openWorkflowTab', openWorkflowTabArguments(hubWorkflow), openWorkflowTabResult(hubWorkflow)),
        textMessage(`${hubWorkflow.name} is open.`)
    );

async function openWorkflowTabs(page: Page, hubWorkflows: HubWorkflowI[]): Promise<AiHubPage> {
    await prepareAiHub(page);

    await stubAgUiRun(page, AI_HUB_CHAT_URL, hubWorkflows.map(openWorkflowTabTurn));

    const aiHubPage = new AiHubPage(page);

    await aiHubPage.goto();

    for (const hubWorkflow of hubWorkflows) {
        await aiHubPage.sendAndWaitForTurnEnd(`Open ${hubWorkflow.name}`);

        await expect(aiHubPage.workflowTab(hubWorkflow.name)).toBeVisible();
    }

    return aiHubPage;
}

test.describe('AI Hub workflow tab', () => {
    test('the tab is the full editor: a node opens for editing and a test run shows its output', async ({
        authenticatedPage: page,
        workflow,
    }) => {
        const hubWorkflow = await toHubWorkflow(page, workflow);

        await openWorkflowTabs(page, [hubWorkflow]);

        await new WorkflowPage(page).waitForCanvasReady();

        await test.step('Open a node', async () => {
            await page.getByLabel('var_1 node', {exact: true}).click();

            await expect(page.getByLabel('var_1 component configuration panel')).toBeVisible();
        });

        await test.step('Run a test and see the output', async () => {
            await page.getByRole('button', {exact: true, name: 'Test'}).click();

            await expect(page.getByText('2 tasks executed')).toBeVisible({timeout: 60000});

            await expect(page.getByText('DONE', {exact: true})).toBeVisible();
        });
    });

    test('publish and deploy work from the tab', async ({authenticatedPage: page, workflow}) => {
        const hubWorkflow = await toHubWorkflow(page, workflow);

        await openWorkflowTabs(page, [hubWorkflow]);

        await new WorkflowPage(page).waitForCanvasReady();

        const deployButton = page.getByRole('button', {exact: true, name: 'Deploy'});

        await test.step('Publish the project', async () => {
            await expect(deployButton).toBeDisabled();

            await page.getByRole('button', {exact: true, name: 'Publish'}).click();

            await page.getByRole('button', {name: 'Publish button'}).click();

            await expect(deployButton).toBeEnabled();
        });

        await test.step('Open the deployment dialog', async () => {
            await deployButton.click();

            await expect(page.getByRole('dialog')).toBeVisible();
        });
    });

    test('re-pointing the tab to another workflow of the project shows its canvas without the previous node details', async ({
        authenticatedPage: page,
        project,
        workflow,
    }) => {
        const otherWorkflow = await createWorkflow(page, project.id);

        const firstHubWorkflow = await toHubWorkflow(page, workflow);
        const secondHubWorkflow = await toHubWorkflow(page, otherWorkflow);

        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            openWorkflowTabTurn(firstHubWorkflow),
            openWorkflowTabTurn(secondHubWorkflow),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await test.step('Open a node in the first workflow', async () => {
            await aiHubPage.sendAndWaitForTurnEnd(`Open ${firstHubWorkflow.name}`);

            await page.getByLabel('var_1 node', {exact: true}).click();

            await expect(page.getByLabel('var_1 component configuration panel')).toBeVisible();
        });

        await test.step('Open the second workflow of the same project', async () => {
            await aiHubPage.sendAndWaitForTurnEnd(`Open ${secondHubWorkflow.name}`);

            await expect(aiHubPage.workflowTab(secondHubWorkflow.name)).toBeVisible();

            await expect(aiHubPage.workflowTab(firstHubWorkflow.name)).toHaveCount(0);

            await expect(page.getByLabel('trigger_1 node', {exact: true})).toBeInViewport();
        });

        await test.step('Assert no node details leaked', async () => {
            await expect(page.getByLabel('var_1 node', {exact: true})).toHaveCount(0);

            await expect(page.getByLabel(/component configuration panel$/)).toHaveCount(0);
        });
    });

    test('the Projects page editor still opens the workflow', async ({authenticatedPage: page, workflow}) => {
        const workflowPage = new WorkflowPage(page);

        await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

        await workflowPage.waitForCanvasReady();

        await page.getByLabel('var_1 node', {exact: true}).click();

        await expect(page.getByLabel('var_1 component configuration panel')).toBeVisible();

        await expect(page.getByRole('button', {exact: true, name: 'Test'})).toBeEnabled();
    });
});
