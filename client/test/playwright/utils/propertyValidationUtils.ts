import {expect, type Locator, type Page} from '@playwright/test';

import {WorkflowPage} from '../pages/workflowPage';
import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';

export async function fillPropertyInput(
    configurationPanel: Locator,
    propertyLabel: string,
    value: string
): Promise<void> {
    const property = configurationPanel.getByLabel(propertyLabel);
    const input = property.getByRole('textbox');
    await input.clear();
    await input.fill(value);
}

export async function assertPropertyValidation(
    configurationPanel: Locator,
    propertyLabel: string,
    expectedError?: string
): Promise<void> {
    const property = configurationPanel.getByLabel(propertyLabel);
    const alert = property.locator('[role="alert"]');
    if (expectedError !== undefined) {
        await expect(alert).toBeVisible();
        await expect(alert).toHaveText(expectedError);
    } else {
        await expect(alert).not.toBeVisible();
    }
}

export async function openPropertyTestingPanelAndPropertiesTab(
    page: Page,
    projectId: string,
    workflowId: string,
    anchorPropertyLabel: string
): Promise<Locator> {
    const workflowPage = new WorkflowPage(page);

    await workflowPage.goToWorkflowEditor(projectId, workflowId);

    const propertyTestingNode = page.getByLabel('propertyTesting_1 node');
    const configurationPanel = page.getByLabel('propertyTesting_1 component configuration panel');

    await clickAndExpectToBeVisible({
        target: configurationPanel,
        trigger: propertyTestingNode,
    });

    const propertiesTabButton = configurationPanel.getByRole('button', {name: 'Properties'});
    const anchorProperty = configurationPanel.getByLabel(anchorPropertyLabel);

    await clickAndExpectToBeVisible({
        target: anchorProperty,
        trigger: propertiesTabButton,
    });

    return configurationPanel;
}
