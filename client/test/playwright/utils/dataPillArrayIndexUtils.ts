import {type Locator, type Page, expect} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {TIMEOUTS} from './constants';
import {propertyTestingParametersSavePromise} from './propertyValidationUtils';

export const VAR_NODE_DATA_PILL_ACCORDION_NAME = 'Var (var_1) set';

export const ARRAY_INDEX_TRIGGER_NAME_REGEX = /^Array index \d+, click to change$/;

export function arrayIndexTriggerName(arrayIndex: number): string {
    return `Array index ${arrayIndex}, click to change`;
}

export function getDataPillPanel(page: Page): Locator {
    return page.locator('main:has(> div > input[name="dataPillFilter"])');
}

export function getArrayItemDataPill(dataPillPanel: Locator, arrayPropertyName: string): Locator {
    return dataPillPanel.locator(`li:has(> [data-name="${arrayPropertyName}"]) > ul [data-name="var_1"]`).first();
}

export function getMentionsInput(configurationPanel: Locator, propertyLabel: string): Locator {
    return configurationPanel.getByLabel(propertyLabel).getByRole('textbox');
}

export function getArrayIndexTriggers(configurationPanel: Locator, propertyLabel: string): Locator {
    return configurationPanel.getByLabel(propertyLabel).getByRole('button', {name: ARRAY_INDEX_TRIGGER_NAME_REGEX});
}

export function getArrayIndexPopover(page: Page): Locator {
    return page.locator('[data-slot="popover-content"][data-state="open"]');
}

interface OpenDataPillPanelProps {
    arrayPropertyName: string;
    configurationPanel: Locator;
    page: Page;
    propertyLabel: string;
}

async function openDataPillPanel({
    arrayPropertyName,
    configurationPanel,
    page,
    propertyLabel,
}: OpenDataPillPanelProps): Promise<Locator> {
    const dataPillPanel = getDataPillPanel(page);

    await clickAndExpectToBeVisible({
        target: dataPillPanel,
        timeout: TIMEOUTS.NODE_DETAILS_PANEL_READY,
        trigger: getMentionsInput(configurationPanel, propertyLabel),
    });

    const arrayItemDataPill = getArrayItemDataPill(dataPillPanel, arrayPropertyName);
    const varNodeAccordionTrigger = dataPillPanel.getByRole('button', {name: VAR_NODE_DATA_PILL_ACCORDION_NAME});

    await expect(async () => {
        const accordionExpanded = await varNodeAccordionTrigger.getAttribute('aria-expanded', {
            timeout: TIMEOUTS.RETRY_CLICK,
        });

        if (accordionExpanded !== 'true') {
            await varNodeAccordionTrigger.click({timeout: TIMEOUTS.RETRY_CLICK});
        }

        await expect(arrayItemDataPill).toBeVisible({timeout: TIMEOUTS.RETRY_VISIBILITY});
    }).toPass({timeout: TIMEOUTS.CLICK_AND_EXPECT});

    return dataPillPanel;
}

interface InsertArrayItemDataPillProps {
    arrayPropertyName: string;
    configurationPanel: Locator;
    expectedReference: string;
    page: Page;
    propertyLabel: string;
}

export async function insertArrayItemDataPill({
    arrayPropertyName,
    configurationPanel,
    expectedReference,
    page,
    propertyLabel,
}: InsertArrayItemDataPillProps): Promise<void> {
    const dataPillPanel = await openDataPillPanel({arrayPropertyName, configurationPanel, page, propertyLabel});

    const saveResponsePromise = propertyTestingParametersSavePromise(page, expectedReference);

    await getArrayItemDataPill(dataPillPanel, arrayPropertyName).click();

    await saveResponsePromise;
}

interface OpenArrayIndexPopoverProps {
    arrayIndexTrigger: Locator;
    page: Page;
}

export async function openArrayIndexPopover({arrayIndexTrigger, page}: OpenArrayIndexPopoverProps): Promise<Locator> {
    const arrayIndexPopover = getArrayIndexPopover(page);

    await clickAndExpectToBeVisible({
        target: arrayIndexPopover,
        trigger: arrayIndexTrigger,
    });

    return arrayIndexPopover;
}

interface ChangeArrayIndexProps {
    arrayIndex: number;
    arrayIndexTrigger: Locator;
    expectedReference: string;
    page: Page;
}

async function expectArrayIndexToChange(arrayIndexTrigger: Locator, arrayIndex: number): Promise<void> {
    await expect(arrayIndexTrigger).not.toHaveAttribute('aria-label', arrayIndexTriggerName(arrayIndex));
}

export async function pickArrayIndex({
    arrayIndex,
    arrayIndexTrigger,
    expectedReference,
    page,
}: ChangeArrayIndexProps): Promise<void> {
    await expectArrayIndexToChange(arrayIndexTrigger, arrayIndex);

    const arrayIndexPopover = await openArrayIndexPopover({arrayIndexTrigger, page});

    const saveResponsePromise = propertyTestingParametersSavePromise(page, expectedReference);

    await arrayIndexPopover.getByRole('option').nth(arrayIndex).click();

    await saveResponsePromise;
}

export async function applyTypedArrayIndex({
    arrayIndex,
    arrayIndexTrigger,
    expectedReference,
    page,
}: ChangeArrayIndexProps): Promise<void> {
    await expectArrayIndexToChange(arrayIndexTrigger, arrayIndex);

    const arrayIndexPopover = await openArrayIndexPopover({arrayIndexTrigger, page});

    const saveResponsePromise = propertyTestingParametersSavePromise(page, expectedReference);

    await arrayIndexPopover.getByRole('spinbutton').fill(`${arrayIndex}`);
    await arrayIndexPopover.getByRole('button', {name: 'Apply'}).click();

    await saveResponsePromise;
}
