import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowTest} from '../../fixtures';

export const test = mergeTests(loginTest(), projectTest, workflowTest);

test.describe('Workflow Editor', () => {
    test('should open trigger configuration panel when clicking Manual trigger', async ({
        authenticatedPage,
        project,
        workflow,
    }) => {
        await test.step('Navigate to workflow editor', async () => {
            await authenticatedPage.goto(`/automation/projects/${project.id}/project-workflows/${workflow.workflowId}`);
        });

        await test.step('Wait for workflow editor to load', async () => {
            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);
        });

        await test.step('Click on the Manual trigger node', async () => {
            const manualTriggerNode = authenticatedPage.getByLabel('trigger_1 node');

            await expect(manualTriggerNode).toBeVisible({timeout: 10000});

            await manualTriggerNode.click();
        });

        await test.step('Assert that trigger configuration panel exists', async () => {
            const triggerPanel = authenticatedPage.getByLabel('trigger_1 component configuration panel');

            await expect(triggerPanel).toBeVisible({timeout: 10000});
        });
    });
});
