import {type Locator, type Page, expect} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {TIMEOUTS} from './constants';
import {VAR_NODE_DATA_PILL_ACCORDION_NAME, getDataPillPanel} from './dataPillArrayIndexUtils';
import {getTaskParameters, getWorkflowDefinition} from './workflowUtils';

export const DATA_PILL_PANEL_ANCHOR_PROPERTY = 'stringMaxLength';

export function getProperty(configurationPanel: Locator, propertyName: string): Locator {
    return configurationPanel.getByLabel(`${propertyName} property`, {exact: true});
}

export function getFormulaSwitch(property: Locator): Locator {
    return property.getByRole('switch', {exact: true, name: 'Formula'});
}

export function getDynamicSwitch(property: Locator): Locator {
    return property.getByRole('switch', {exact: true, name: 'Dynamic'});
}

export function getPropertyEditor(property: Locator): Locator {
    return property.locator('[contenteditable="true"][role="textbox"]');
}

export function getNativeInput(property: Locator): Locator {
    return property.locator('input').first();
}

export function getPropertyMentions(property: Locator): Locator {
    return property.locator('.property-mention[data-id]');
}

export function getSuggestionMenu(page: Page): Locator {
    return page.locator('.property-mentions-suggestion-menu');
}

export function getVarDataPill(dataPillPanel: Locator, pillName: string): Locator {
    return dataPillPanel.locator(`[data-name="${pillName}"]`).first();
}

export async function openVarDataPills(page: Page, configurationPanel: Locator): Promise<Locator> {
    const dataPillPanel = getDataPillPanel(page);

    await clickAndExpectToBeVisible({
        target: dataPillPanel,
        timeout: TIMEOUTS.NODE_DETAILS_PANEL_READY,
        trigger: getPropertyEditor(getProperty(configurationPanel, DATA_PILL_PANEL_ANCHOR_PROPERTY)),
    });

    const varNodeAccordionTrigger = dataPillPanel.getByRole('button', {name: VAR_NODE_DATA_PILL_ACCORDION_NAME});
    const numberDataPill = getVarDataPill(dataPillPanel, 'Number');

    await expect(async () => {
        const accordionExpanded = await varNodeAccordionTrigger.getAttribute('aria-expanded', {
            timeout: TIMEOUTS.RETRY_CLICK,
        });

        if (accordionExpanded !== 'true') {
            await varNodeAccordionTrigger.click({timeout: TIMEOUTS.RETRY_CLICK});
        }

        await expect(numberDataPill).toBeVisible({timeout: TIMEOUTS.RETRY_VISIBILITY});
    }).toPass({timeout: TIMEOUTS.CLICK_AND_EXPECT});

    return dataPillPanel;
}

export async function getStoredParameter(page: Page, workflowId: string, propertyName: string): Promise<unknown> {
    const workflowDefinition = await getWorkflowDefinition(page, workflowId);

    const parameters = getTaskParameters({taskName: 'propertyTesting_1', workflowDefinition});

    return parameters?.[propertyName] ?? null;
}

interface ExpectStoredParameterProps {
    expected: unknown;
    page: Page;
    propertyName: string;
    workflowId: string;
}

export async function expectStoredParameter({
    expected,
    page,
    propertyName,
    workflowId,
}: ExpectStoredParameterProps): Promise<void> {
    await expect.poll(() => getStoredParameter(page, workflowId, propertyName), {timeout: 20000}).toEqual(expected);
}
