import {type Locator, type Page, expect} from '@playwright/test';

import {clickAndExpectToBeVisible} from './clickAndExpectToBeVisible';
import {propertyTestingParametersSavePromise} from './propertyValidationUtils';

export const BOOLEAN_PROPERTY_LABEL = 'bool property';
export const CONDITIONAL_PROPERTY_LABEL = 'displayCondition property';

interface SetBooleanPropertyValueProps {
    configurationPanel: Locator;
    page: Page;
    value: boolean;
}

export async function setBooleanPropertyValue({
    configurationPanel,
    page,
    value,
}: SetBooleanPropertyValueProps): Promise<void> {
    const booleanProperty = configurationPanel.getByLabel(BOOLEAN_PROPERTY_LABEL, {exact: true});

    const booleanSelect = booleanProperty.getByLabel('Select');

    const option = page.getByRole('option', {exact: true, name: value ? 'True' : 'False'});

    await clickAndExpectToBeVisible({
        autoClick: true,
        target: option,
        trigger: booleanSelect,
    });

    await expect(booleanSelect).toHaveText(value ? 'True' : 'False');
}

export async function setBooleanPropertyValueAndWaitForSave({
    configurationPanel,
    page,
    value,
}: SetBooleanPropertyValueProps): Promise<void> {
    const saveResponsePromise = propertyTestingParametersSavePromise(page);

    await setBooleanPropertyValue({configurationPanel, page, value});

    await saveResponsePromise;
}

export function getConditionalProperty(configurationPanel: Locator): Locator {
    return configurationPanel.getByLabel(CONDITIONAL_PROPERTY_LABEL, {exact: true});
}
