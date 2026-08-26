import {type Locator, type Page, expect} from '@playwright/test';

import {WorkflowPage} from '../pages/workflowPage';
import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {TIMEOUTS} from './constants';
import {nodeParametersSavePromise} from './workflowUtils';

export function propertyTestingParametersSavePromise(page: Page, valueInBody?: string) {
    return nodeParametersSavePromise({nodeName: 'propertyTesting_1', page, valueInBody});
}

export function propertyTestingParametersDeletePromise(page: Page) {
    return nodeParametersSavePromise({method: 'DELETE', nodeName: 'propertyTesting_1', page});
}

interface FillInputAndWaitForSaveProps {
    input: Locator;
    page: Page;
    value: string;
}

export async function fillInputAndWaitForSave({input, page, value}: FillInputAndWaitForSaveProps): Promise<void> {
    const saveResponsePromise = propertyTestingParametersSavePromise(page, value);

    await input.fill(value);

    await saveResponsePromise;
}

interface FillPropertyInputProps {
    configurationPanel: Locator;
    inputRole?: 'textbox' | 'spinbutton';
    propertyLabel: string;
    value: string;
}

export async function fillPropertyInput({
    configurationPanel,
    inputRole = 'textbox',
    propertyLabel,
    value,
}: FillPropertyInputProps): Promise<void> {
    const property = configurationPanel.getByLabel(propertyLabel);

    const input = property.getByRole(inputRole);

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

export async function openPropertyTestingPanel(page: Page, anchorPropertyLabel: string): Promise<Locator> {
    const propertyTestingNode = page.getByLabel('propertyTesting_1 node', {exact: true});
    const configurationPanel = page.getByLabel('propertyTesting_1 component configuration panel');

    // Also reached straight after a reload, where the canvas needs several seconds to mount its nodes.
    await expect(propertyTestingNode).toBeVisible({timeout: TIMEOUTS.EDITOR_CANVAS_READY});

    await clickAndExpectToBeVisible({
        target: configurationPanel,
        trigger: propertyTestingNode,
    });

    const propertiesTabButton = configurationPanel.getByRole('button', {name: 'Properties'});
    const anchorProperty = configurationPanel.getByLabel(anchorPropertyLabel);

    // The panel renders a skeleton tab row until the operation definition resolves, so the Properties
    // button does not exist yet. Wait it out here rather than inside the short retry budget below.
    await expect(propertiesTabButton).toBeVisible({timeout: TIMEOUTS.NODE_DETAILS_PANEL_READY});

    await clickAndExpectToBeVisible({
        target: anchorProperty,
        trigger: propertiesTabButton,
    });

    return configurationPanel;
}

export async function openPropertyTestingPanelAndPropertiesTab(
    page: Page,
    projectId: string,
    workflowId: string,
    anchorPropertyLabel: string
): Promise<Locator> {
    const workflowPage = new WorkflowPage(page);

    await workflowPage.goToWorkflowEditor(projectId, workflowId);

    return openPropertyTestingPanel(page, anchorPropertyLabel);
}
