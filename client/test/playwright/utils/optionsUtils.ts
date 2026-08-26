import {type Locator, type Page, expect} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {propertyTestingParametersDeletePromise, propertyTestingParametersSavePromise} from './propertyValidationUtils';

export const OPTIONS_SELECT_LABEL = 'optionsNoMultiselect property';
export const OPTIONS_MULTISELECT_LABEL = 'optionsMultiselect property';
export const OPTIONS_LOOKUP_LABEL = 'optionsLookupDependsOn property';
export const OPTIONS_LOOKUP_DEPENDENCY_LABEL = 'setForOptionsLookup property';

export const OPTION_LABELS = ['option1', 'option2', 'option3', 'option4'];

export const RESET_OPTION_LABEL = 'Select...';

// The lookup dependency is unresolved until `setForOptionsLookup` has a value, which is what the
// combobox renders instead of a placeholder.
export const DEPENDENCY_MISSING_PLACEHOLDER = 'setForOptionsLookup is not defined';

// GET issued to resolve the options of a property whose options depend on another property.
const OPTIONS_URL_FRAGMENT = '/workflow-nodes/propertyTesting_1/options/optionsLookupDependsOn';

export const OPTIONS_LOOKUP_ROUTE = /\/workflow-nodes\/propertyTesting_1\/options\/optionsLookupDependsOn/;

export function optionsRequestPromise(page: Page) {
    return page.waitForRequest((request) => request.method() === 'GET' && request.url().includes(OPTIONS_URL_FRAGMENT));
}

export function getComboBoxTrigger(configurationPanel: Locator, propertyLabel: string): Locator {
    return configurationPanel.getByLabel(propertyLabel, {exact: true}).getByRole('combobox');
}

export function getMultiSelectTrigger(configurationPanel: Locator): Locator {
    return configurationPanel.getByLabel(OPTIONS_MULTISELECT_LABEL, {exact: true}).getByRole('button');
}

interface OpenComboBoxProps {
    configurationPanel: Locator;
    page: Page;
    propertyLabel: string;
}

export async function openComboBox({configurationPanel, page, propertyLabel}: OpenComboBoxProps): Promise<void> {
    await clickAndExpectToBeVisible({
        target: page.getByPlaceholder('Search...'),
        trigger: getComboBoxTrigger(configurationPanel, propertyLabel),
    });
}

interface SelectComboBoxOptionProps {
    configurationPanel: Locator;
    optionLabel: string;
    page: Page;
    propertyLabel: string;
    waitForSave?: boolean;
}

export async function selectComboBoxOption({
    configurationPanel,
    optionLabel,
    page,
    propertyLabel,
    waitForSave = true,
}: SelectComboBoxOptionProps): Promise<void> {
    const saveResponsePromise = waitForSave
        ? optionLabel === RESET_OPTION_LABEL
            ? propertyTestingParametersDeletePromise(page)
            : propertyTestingParametersSavePromise(page)
        : undefined;

    const searchInput = page.getByPlaceholder('Search...');

    // Saving a selection re-renders the combobox, so the popover and its options can detach mid-click.
    await expect(async () => {
        if (!(await searchInput.isVisible())) {
            await getComboBoxTrigger(configurationPanel, propertyLabel).click();
        }

        await expect(searchInput).toBeVisible({timeout: 100});

        await page.getByRole('option', {exact: true, name: optionLabel}).click({timeout: 2000});

        await expect(searchInput).toBeHidden({timeout: 2000});
    }).toPass();

    if (saveResponsePromise) {
        await saveResponsePromise;
    }
}

interface OpenMultiSelectProps {
    configurationPanel: Locator;
    page: Page;
}

export async function openMultiSelect({configurationPanel, page}: OpenMultiSelectProps): Promise<void> {
    await clickAndExpectToBeVisible({
        target: page.getByRole('option', {name: 'Select All'}),
        trigger: getMultiSelectTrigger(configurationPanel),
    });
}

interface ToggleMultiSelectOptionsProps {
    configurationPanel: Locator;
    optionLabels: Array<string>;
    page: Page;
}

export async function toggleMultiSelectOptions({
    configurationPanel,
    optionLabels,
    page,
}: ToggleMultiSelectOptionsProps): Promise<void> {
    await openMultiSelect({configurationPanel, page});

    for (const optionLabel of optionLabels) {
        const saveResponsePromise = propertyTestingParametersSavePromise(page);

        await page.getByRole('option', {exact: true, name: optionLabel}).click();

        await saveResponsePromise;
    }

    await page.keyboard.press('Escape');
}

export function getSelectedMultiSelectBadge(configurationPanel: Locator, optionLabel: string): Locator {
    return configurationPanel.getByLabel(`${optionLabel}-selected`);
}

interface SetOptionsLookupDependencyProps {
    configurationPanel: Locator;
    page: Page;
    value: string;
    waitForRequest?: boolean;
}

export async function setOptionsLookupDependency({
    configurationPanel,
    page,
    value,
    waitForRequest = true,
}: SetOptionsLookupDependencyProps): Promise<void> {
    const dependencyProperty = configurationPanel.getByLabel(OPTIONS_LOOKUP_DEPENDENCY_LABEL, {exact: true});

    const dependencyInput = dependencyProperty.getByRole('textbox');

    const requestPromise = waitForRequest ? optionsRequestPromise(page) : undefined;

    await dependencyInput.clear();
    await dependencyInput.fill(value);

    if (requestPromise) {
        await requestPromise;
    }
}

export async function assertComboBoxValue(
    configurationPanel: Locator,
    propertyLabel: string,
    expectedLabel: string
): Promise<void> {
    await expect(getComboBoxTrigger(configurationPanel, propertyLabel)).toHaveText(expectedLabel);
}
