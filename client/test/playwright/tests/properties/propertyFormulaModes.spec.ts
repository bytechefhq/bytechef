import {type Locator, expect, mergeTests} from '@playwright/test';

import {importPropertyFormulaModesWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {getDataPillPanel} from '../../utils/dataPillArrayIndexUtils';
import {
    expectStoredParameter,
    getDynamicSwitch,
    getFormulaSwitch,
    getNativeInput,
    getProperty,
    getPropertyEditor,
    getPropertyMentions,
    getSuggestionMenu,
    getVarDataPill,
    openVarDataPills,
} from '../../utils/formulaModesUtils';
import {
    openPropertyTestingPanel,
    openPropertyTestingPanelAndPropertiesTab,
    propertyTestingParametersSavePromise,
} from '../../utils/propertyValidationUtils';

export const test = mergeTests(loginTest(), projectTest, importPropertyFormulaModesWorkflowTest);

const ANCHOR_PROPERTY_LABEL = 'numberPrecision property';

const NUMBER_PILL_REFERENCE = '${var_1.Number}';

const STRING_PILL_REFERENCE = '${var_1.String}';

const FLAG_PILL_REFERENCE = '${var_1.Flag}';

test.describe('Property formula modes', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            ANCHOR_PROPERTY_LABEL
        );
    });

    test.describe('Formula switch visibility', () => {
        test('should show the Formula switch on string, number, integer, boolean, select and date properties', async () => {
            for (const propertyName of [
                'stringMinLength',
                'numberPrecision',
                'integerMaxValue',
                'bool',
                'optionsNoMultiselect',
                'date',
            ]) {
                await expect(getFormulaSwitch(getProperty(configurationPanel, propertyName))).toBeVisible();
            }
        });

        test('should hide the Formula switch on a file entry property', async () => {
            const fileEntryProperty = getProperty(configurationPanel, 'fileEntry');

            await expect(getPropertyEditor(fileEntryProperty)).toBeVisible();
            await expect(getFormulaSwitch(fileEntryProperty)).toHaveCount(0);
        });
    });

    test.describe('Switching with value conversion', () => {
        test('should turn a constant number into a formula and back', async ({authenticatedPage: page, workflow}) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            await test.step('Type a constant number', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, '5');

                await getNativeInput(numberProperty).fill('5');

                await saveResponsePromise;
            });

            await test.step('Switch to Formula', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, '=5');

                await getFormulaSwitch(numberProperty).click();

                await saveResponsePromise;

                await expect(getFormulaSwitch(numberProperty)).toBeChecked();
                await expect(getPropertyEditor(numberProperty)).toHaveText('5');

                await expectStoredParameter({
                    expected: '=5',
                    page,
                    propertyName: 'numberPrecision',
                    workflowId: workflow.workflowId,
                });
            });

            await test.step('Switch back to Text', async () => {
                await getFormulaSwitch(numberProperty).click();

                await expect(getFormulaSwitch(numberProperty)).not.toBeChecked();
                await expect(getNativeInput(numberProperty)).toHaveValue('5');

                await expectStoredParameter({
                    expected: 5,
                    page,
                    propertyName: 'numberPrecision',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should keep a lone data pill when switching to Formula and back', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Insert a data pill into the number field', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

                await getNativeInput(numberProperty).click();
                await getVarDataPill(dataPillPanel, 'Number').click();

                await saveResponsePromise;
            });

            await test.step('Switch to Formula', async () => {
                await getFormulaSwitch(numberProperty).click();

                await expect(getFormulaSwitch(numberProperty)).toBeChecked();
                await expect(getPropertyMentions(numberProperty)).toHaveAttribute('data-id', 'var_1.Number');

                await expectStoredParameter({
                    expected: `=${NUMBER_PILL_REFERENCE}`,
                    page,
                    propertyName: 'numberPrecision',
                    workflowId: workflow.workflowId,
                });
            });

            await test.step('Switch back to Text', async () => {
                await getFormulaSwitch(numberProperty).click();

                await expect(getFormulaSwitch(numberProperty)).not.toBeChecked();
                await expect(getPropertyMentions(numberProperty)).toHaveCount(1);

                await expectStoredParameter({
                    expected: NUMBER_PILL_REFERENCE,
                    page,
                    propertyName: 'numberPrecision',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should clear text mixed with a data pill when switching to Formula', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const stringProperty = getProperty(configurationPanel, 'stringMinLength');
            const stringEditor = getPropertyEditor(stringProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Type text and insert a data pill', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(
                    page,
                    `hello ${STRING_PILL_REFERENCE}`
                );

                await stringEditor.click();

                await page.keyboard.type('hello ');

                await getVarDataPill(dataPillPanel, 'String').click();

                await saveResponsePromise;
            });

            await test.step('Switch to Formula', async () => {
                await getFormulaSwitch(stringProperty).click();

                await expect(getFormulaSwitch(stringProperty)).toBeChecked();
                await expect(stringEditor).toHaveText('');

                await expectStoredParameter({
                    expected: null,
                    page,
                    propertyName: 'stringMinLength',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should clear a formula that is not a literal when switching back to Text', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            await getFormulaSwitch(numberProperty).click();

            await expect(getPropertyEditor(numberProperty)).toBeFocused();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, '=foo(1)');

            await page.keyboard.type('foo(1)');

            await saveResponsePromise;

            await getFormulaSwitch(numberProperty).click();

            await expect(getNativeInput(numberProperty)).toHaveValue('');

            await expectStoredParameter({
                expected: null,
                page,
                propertyName: 'numberPrecision',
                workflowId: workflow.workflowId,
            });
        });

        test('should turn a boolean literal formula into the boolean value', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            await getFormulaSwitch(boolProperty).click();

            await expect(getPropertyEditor(boolProperty)).toBeFocused();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, '=true');

            await page.keyboard.type('true');

            await saveResponsePromise;

            await getFormulaSwitch(boolProperty).click();

            await expect(boolProperty.getByRole('combobox')).toHaveText('True');

            await expectStoredParameter({expected: true, page, propertyName: 'bool', workflowId: workflow.workflowId});
        });

        test('should turn a quoted string formula into the text', async ({authenticatedPage: page, workflow}) => {
            const stringProperty = getProperty(configurationPanel, 'stringMinLength');

            await getFormulaSwitch(stringProperty).click();

            await expect(getPropertyEditor(stringProperty)).toBeFocused();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, "='hello'");

            await page.keyboard.type("'hello'");

            await saveResponsePromise;

            await getFormulaSwitch(stringProperty).click();

            await expect(getPropertyEditor(stringProperty)).toHaveText('hello');

            await expectStoredParameter({
                expected: 'hello',
                page,
                propertyName: 'stringMinLength',
                workflowId: workflow.workflowId,
            });
        });

        test('should focus the formula editor when switching on and the native input when switching off', async () => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            await getFormulaSwitch(numberProperty).click();

            await expect(getPropertyEditor(numberProperty)).toBeFocused();

            await getFormulaSwitch(numberProperty).click();

            await expect(getNativeInput(numberProperty)).toBeFocused();
        });

        test('should leave Formula mode on Backspace in an empty formula', async ({authenticatedPage: page}) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            await getFormulaSwitch(numberProperty).click();

            await expect(getPropertyEditor(numberProperty)).toBeFocused();

            await page.keyboard.press('Backspace');

            await expect(getFormulaSwitch(numberProperty)).not.toBeChecked();
            await expect(getNativeInput(numberProperty)).toBeVisible();
        });
    });

    test.describe('Keyboard handover', () => {
        for (const {propertyName, typeLabel} of [
            {propertyName: 'numberPrecision', typeLabel: 'number'},
            {propertyName: 'integerMaxValue', typeLabel: 'integer'},
        ]) {
            test(`should open the data pill editor with its suggestions on $ in an empty ${typeLabel} field`, async ({
                authenticatedPage: page,
            }) => {
                const property = getProperty(configurationPanel, propertyName);

                await getNativeInput(property).click();

                await page.keyboard.press('$');

                await expect(getPropertyEditor(property)).toBeFocused();
                await expect(getPropertyEditor(property)).toHaveText('$');
                await expect(getSuggestionMenu(page)).toBeVisible();
            });

            test(`should switch to Formula on = in an empty ${typeLabel} field`, async ({authenticatedPage: page}) => {
                const property = getProperty(configurationPanel, propertyName);

                await getNativeInput(property).click();

                await page.keyboard.press('=');

                await expect(getFormulaSwitch(property)).toBeChecked();
                await expect(getPropertyEditor(property)).toBeFocused();
            });
        }

        test('should not switch modes on $ or = in a number field that holds a value', async ({
            authenticatedPage: page,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');
            const numberInput = getNativeInput(numberProperty);

            const saveResponsePromise = propertyTestingParametersSavePromise(page, '5');

            await numberInput.fill('5');

            await saveResponsePromise;

            await numberInput.press('End');
            await numberInput.press('$');
            await numberInput.press('=');

            await expect(numberInput).toBeVisible();
            await expect(getPropertyEditor(numberProperty)).toHaveCount(0);
            await expect(getFormulaSwitch(numberProperty)).not.toBeChecked();
        });

        test('should switch to Formula on = in an empty string field', async ({authenticatedPage: page}) => {
            const stringProperty = getProperty(configurationPanel, 'stringMinLength');

            await getPropertyEditor(stringProperty).click();

            await page.keyboard.press('=');

            await expect(getFormulaSwitch(stringProperty)).toBeChecked();
            await expect(getPropertyEditor(stringProperty)).toBeFocused();
        });
    });

    test.describe('One-pill editor', () => {
        test('should take only $ in an empty one-pill editor', async ({authenticatedPage: page}) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            await getDynamicSwitch(boolProperty).click();

            const boolEditor = getPropertyEditor(boolProperty);

            await expect(boolEditor).toBeFocused();

            await page.keyboard.type('abc');

            await expect(boolEditor).toHaveText('');

            await page.keyboard.press('$');

            await expect(boolEditor).toHaveText('$');
            await expect(getSuggestionMenu(page)).toBeVisible();
        });

        test('should ignore typed text next to a data pill and replace the pill with a second one', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');
            const numberEditor = getPropertyEditor(numberProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Insert a data pill', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

                await getNativeInput(numberProperty).click();
                await getVarDataPill(dataPillPanel, 'Number').click();

                await saveResponsePromise;
            });

            await test.step('Type next to the pill', async () => {
                await numberEditor.click();
                await numberEditor.press('End');

                await page.keyboard.type('abc');

                await expect(numberEditor).toHaveText('var_1.Number');
            });

            await test.step('Insert a second data pill', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

                await getVarDataPill(dataPillPanel, 'String').click();

                await saveResponsePromise;

                await expect(getPropertyMentions(numberProperty)).toHaveCount(1);
                await expect(getPropertyMentions(numberProperty)).toHaveText('var_1.String');

                await expectStoredParameter({
                    expected: STRING_PILL_REFERENCE,
                    page,
                    propertyName: 'numberPrecision',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should empty the value and bring back the native input when the data pill is deleted', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            const saveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

            await getNativeInput(numberProperty).click();
            await getVarDataPill(dataPillPanel, 'Number').click();

            await saveResponsePromise;

            const numberEditor = getPropertyEditor(numberProperty);

            await numberEditor.click();
            await numberEditor.press('End');
            await numberEditor.press('Backspace');

            await expect(getNativeInput(numberProperty)).toBeVisible();
            await expect(getNativeInput(numberProperty)).toHaveValue('');
            await expect(numberEditor).toHaveCount(0);

            await expectStoredParameter({
                expected: null,
                page,
                propertyName: 'numberPrecision',
                workflowId: workflow.workflowId,
            });
        });

        test('should focus the native input that comes back when the data pill is deleted', async ({
            authenticatedPage: page,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            const saveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

            await getNativeInput(numberProperty).click();
            await getVarDataPill(dataPillPanel, 'Number').click();

            await saveResponsePromise;

            const numberEditor = getPropertyEditor(numberProperty);

            await numberEditor.click();
            await numberEditor.press('End');
            await numberEditor.press('Backspace');

            await expect(getNativeInput(numberProperty)).toBeFocused({timeout: 5000});
        });
    });

    test.describe('Pill target', () => {
        test('should put a clicked data pill into the focused select instead of the editor focused before', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const selectProperty = getProperty(configurationPanel, 'optionsNoMultiselect');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await selectProperty.getByRole('combobox').focus();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'String').click();

            await saveResponsePromise;

            await expect(getPropertyMentions(selectProperty)).toHaveAttribute('data-id', 'var_1.String');
            await expect(getPropertyEditor(getProperty(configurationPanel, 'stringMaxLength'))).toHaveText('');

            await expectStoredParameter({
                expected: STRING_PILL_REFERENCE,
                page,
                propertyName: 'optionsNoMultiselect',
                workflowId: workflow.workflowId,
            });
            await expectStoredParameter({
                expected: null,
                page,
                propertyName: 'stringMaxLength',
                workflowId: workflow.workflowId,
            });
        });

        test('should replace the constant of a select with a dropped data pill', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Select True', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, 'true');

                await boolProperty.getByRole('combobox').click();
                await page.getByRole('option', {exact: true, name: 'True'}).click();

                await saveResponsePromise;
            });

            await test.step('Drop a data pill onto the select', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, FLAG_PILL_REFERENCE);

                await getVarDataPill(dataPillPanel, 'Flag').dragTo(boolProperty.getByRole('combobox'));

                await saveResponsePromise;

                await expect(getPropertyMentions(boolProperty)).toHaveAttribute('data-id', 'var_1.Flag');

                await expectStoredParameter({
                    expected: FLAG_PILL_REFERENCE,
                    page,
                    propertyName: 'bool',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should insert a clicked data pill into the formula text', async ({authenticatedPage: page, workflow}) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');
            const numberEditor = getPropertyEditor(numberProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await getFormulaSwitch(numberProperty).click();

            await expect(numberEditor).toBeFocused();

            await page.keyboard.type('1 + ');

            const saveResponsePromise = propertyTestingParametersSavePromise(page, `=1 + ${NUMBER_PILL_REFERENCE}`);

            await getVarDataPill(dataPillPanel, 'Number').click();

            await saveResponsePromise;

            await expect(numberEditor).toContainText('1 +');
            await expect(getPropertyMentions(numberProperty)).toHaveAttribute('data-id', 'var_1.Number');

            await expectStoredParameter({
                expected: `=1 + ${NUMBER_PILL_REFERENCE}`,
                page,
                propertyName: 'numberPrecision',
                workflowId: workflow.workflowId,
            });
        });
    });

    test.describe('Selects', () => {
        test('should open an empty data pill editor on $ in a select that holds an option', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            await test.step('Select True', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, 'true');

                await boolProperty.getByRole('combobox').click();
                await page.getByRole('option', {exact: true, name: 'True'}).click();

                await saveResponsePromise;
            });

            await test.step('Type $ on the select', async () => {
                await boolProperty.getByRole('combobox').focus();

                await page.keyboard.press('$');

                await expect(getPropertyEditor(boolProperty)).toBeFocused();
                await expect(getPropertyEditor(boolProperty)).toHaveText('$');
                await expect(getDynamicSwitch(boolProperty)).toBeChecked();
            });

            await test.step('Erase the $ and turn Dynamic off', async () => {
                await page.keyboard.press('Backspace');

                await getDynamicSwitch(boolProperty).click();

                await expect(boolProperty.getByRole('combobox')).toHaveText('True');

                await expectStoredParameter({
                    expected: true,
                    page,
                    propertyName: 'bool',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should replace the option of a select with a pill picked after $', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            const saveResponsePromise = propertyTestingParametersSavePromise(page, 'true');

            await boolProperty.getByRole('combobox').click();
            await page.getByRole('option', {exact: true, name: 'True'}).click();

            await saveResponsePromise;

            await boolProperty.getByRole('combobox').focus();

            await page.keyboard.press('$');

            await expect(getSuggestionMenu(page)).toBeVisible();

            const pillSaveResponsePromise = propertyTestingParametersSavePromise(page, FLAG_PILL_REFERENCE);

            await getSuggestionMenu(page).getByRole('button', {name: 'var_1.Flag'}).click();

            await pillSaveResponsePromise;

            await expect(getPropertyMentions(boolProperty)).toHaveAttribute('data-id', 'var_1.Flag');

            await expectStoredParameter({
                expected: FLAG_PILL_REFERENCE,
                page,
                propertyName: 'bool',
                workflowId: workflow.workflowId,
            });
        });

        test('should not switch a select to Formula on =', async ({authenticatedPage: page}) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            await boolProperty.getByRole('combobox').focus();

            await page.keyboard.press('=');

            await expect(getFormulaSwitch(boolProperty)).not.toBeChecked();
            await expect(boolProperty.getByRole('combobox')).toBeVisible();
            await expect(getPropertyEditor(boolProperty)).toHaveCount(0);
        });

        test('should take text and several data pills in a string select and clear it on blur once the pills are gone', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const selectProperty = getProperty(configurationPanel, 'optionsNoMultiselect');
            const selectEditor = getPropertyEditor(selectProperty);

            await test.step('Turn Dynamic on and enter text with two data pills', async () => {
                await getDynamicSwitch(selectProperty).click();

                await expect(selectEditor).toBeFocused();

                await page.keyboard.type('id $');

                await getSuggestionMenu(page).getByRole('button', {name: 'var_1.String'}).click();

                const saveResponsePromise = propertyTestingParametersSavePromise(
                    page,
                    `id ${STRING_PILL_REFERENCE}${NUMBER_PILL_REFERENCE}`
                );

                await page.keyboard.press('$');

                await getSuggestionMenu(page).getByRole('button', {name: 'var_1.Number'}).click();

                await saveResponsePromise;

                await expect(selectEditor).toHaveText('id var_1.Stringvar_1.Number');
                await expect(getPropertyMentions(selectProperty)).toHaveCount(2);
            });

            await test.step('Delete both data pills', async () => {
                await selectEditor.press('End');
                await selectEditor.press('Backspace');
                await selectEditor.press('Backspace');

                await expect(getPropertyMentions(selectProperty)).toHaveCount(0);
                await expect(selectEditor).toBeFocused();
                await expect(selectEditor).toHaveText('id ');
            });

            await test.step('Leave the editor', async () => {
                await getProperty(configurationPanel, 'stringMinLength').getByText('String Min Length').click();

                await expect(selectEditor).toHaveText('');
                await expect(getDynamicSwitch(selectProperty)).toBeChecked();

                await expectStoredParameter({
                    expected: null,
                    page,
                    propertyName: 'optionsNoMultiselect',
                    workflowId: workflow.workflowId,
                });
            });

            await test.step('Turn Dynamic off', async () => {
                await getDynamicSwitch(selectProperty).click();

                await expect(selectProperty.getByRole('combobox')).toBeVisible();
            });
        });

        test('should keep typed text when a data pill is clicked in the panel of a string select', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const selectProperty = getProperty(configurationPanel, 'optionsNoMultiselect');
            const selectEditor = getPropertyEditor(selectProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await getDynamicSwitch(selectProperty).click();

            await expect(selectEditor).toBeFocused();

            await page.keyboard.type('id ');

            await getVarDataPill(dataPillPanel, 'String').click();

            await expect(selectEditor).toHaveText('id var_1.String', {timeout: 5000});

            await expectStoredParameter({
                expected: `id ${STRING_PILL_REFERENCE}`,
                page,
                propertyName: 'optionsNoMultiselect',
                workflowId: workflow.workflowId,
            });
        });

        test('should keep one data pill in a boolean select', async ({authenticatedPage: page, workflow}) => {
            const boolProperty = getProperty(configurationPanel, 'bool');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await boolProperty.getByRole('combobox').focus();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, FLAG_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'Flag').click();

            await saveResponsePromise;

            const secondSaveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'String').click();

            await secondSaveResponsePromise;

            await expect(getPropertyMentions(boolProperty)).toHaveCount(1);
            await expect(getPropertyMentions(boolProperty)).toHaveText('var_1.String');

            await expectStoredParameter({
                expected: STRING_PILL_REFERENCE,
                page,
                propertyName: 'bool',
                workflowId: workflow.workflowId,
            });
        });

        test('should insert a data pill suggestion clicked in the popup', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const numberProperty = getProperty(configurationPanel, 'numberPrecision');

            await getNativeInput(numberProperty).click();

            await page.keyboard.press('$');

            await expect(getSuggestionMenu(page)).toBeVisible();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

            await getSuggestionMenu(page).getByRole('button', {name: 'var_1.Number'}).click();

            await saveResponsePromise;

            await expect(getPropertyMentions(numberProperty)).toHaveAttribute('data-id', 'var_1.Number');

            await expectStoredParameter({
                expected: NUMBER_PILL_REFERENCE,
                page,
                propertyName: 'numberPrecision',
                workflowId: workflow.workflowId,
            });
        });
    });

    test.describe('Dynamic switch', () => {
        test('should show the Dynamic switch on select, array and object properties only', async () => {
            for (const propertyName of [
                'bool',
                'optionsNoMultiselect',
                'arrayNoDefaultValues',
                'objectNoDefaultValues',
            ]) {
                await expect(getDynamicSwitch(getProperty(configurationPanel, propertyName))).toBeVisible();
            }

            for (const propertyName of ['stringMinLength', 'numberPrecision', 'integerMaxValue', 'date']) {
                await expect(getDynamicSwitch(getProperty(configurationPanel, propertyName))).toHaveCount(0);
            }
        });

        test('should keep the emptied data pill editor until Dynamic is turned off', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const objectProperty = getProperty(configurationPanel, 'objectNoDefaultValues');
            const objectEditor = getPropertyEditor(objectProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Turn Dynamic on and insert a data pill', async () => {
                await getDynamicSwitch(objectProperty).click();

                await expect(objectEditor).toBeFocused();

                const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

                await getVarDataPill(dataPillPanel, 'String').click();

                await saveResponsePromise;
            });

            await test.step('Delete the data pill', async () => {
                await objectEditor.click();
                await objectEditor.press('End');
                await objectEditor.press('Backspace');

                await expect(getPropertyMentions(objectProperty)).toHaveCount(0);
                await expect(objectEditor).toBeVisible();
                await expect(getDynamicSwitch(objectProperty)).toBeChecked();

                await expectStoredParameter({
                    expected: null,
                    page,
                    propertyName: 'objectNoDefaultValues',
                    workflowId: workflow.workflowId,
                });
            });

            await test.step('Turn Dynamic off', async () => {
                await getDynamicSwitch(objectProperty).click();

                await expect(objectEditor).toHaveCount(0);
                await expect(
                    objectProperty.getByRole('button', {name: 'Add objectNoDefaultValues object property'})
                ).toBeVisible();

                await expectStoredParameter({
                    expected: null,
                    page,
                    propertyName: 'objectNoDefaultValues',
                    workflowId: workflow.workflowId,
                });
            });
        });

        test('should clear the stored value when Dynamic is turned off right after the data pill is deleted', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const objectProperty = getProperty(configurationPanel, 'objectNoDefaultValues');
            const objectEditor = getPropertyEditor(objectProperty);

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await getDynamicSwitch(objectProperty).click();

            await expect(objectEditor).toBeFocused();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'String').click();

            await saveResponsePromise;

            await objectEditor.click();
            await objectEditor.press('End');
            await objectEditor.press('Backspace');

            await getDynamicSwitch(objectProperty).click();

            await expect(objectEditor).toHaveCount(0);

            await expectStoredParameter({
                expected: null,
                page,
                propertyName: 'objectNoDefaultValues',
                workflowId: workflow.workflowId,
            });
        });

        test('should clear a saved data pill when Dynamic is turned off', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const arrayProperty = getProperty(configurationPanel, 'arrayNoDefaultValues');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await getDynamicSwitch(arrayProperty).click();

            await expect(getPropertyEditor(arrayProperty)).toBeFocused();

            const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'String').click();

            await saveResponsePromise;

            await getDynamicSwitch(arrayProperty).click();

            await expect(getPropertyEditor(arrayProperty)).toHaveCount(0);
            await expect(arrayProperty.getByRole('button', {name: 'Add array item'})).toBeVisible();

            await expectStoredParameter({
                expected: null,
                page,
                propertyName: 'arrayNoDefaultValues',
                workflowId: workflow.workflowId,
            });
        });
    });

    test.describe('Containers', () => {
        test('should pick an empty array as pill target from its label and take a pill as its whole value', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const arrayProperty = getProperty(configurationPanel, 'arrayNoDefaultValues');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await arrayProperty.getByText('Array With No Default Values').click();

            await expect(getDataPillPanel(page)).toBeVisible();
            await expect(arrayProperty.getByLabel(/Array property item at index/)).toHaveCount(0);

            const saveResponsePromise = propertyTestingParametersSavePromise(page, STRING_PILL_REFERENCE);

            await getVarDataPill(dataPillPanel, 'String').click();

            await saveResponsePromise;

            await expect(getPropertyMentions(arrayProperty)).toHaveAttribute('data-id', 'var_1.String');

            await expectStoredParameter({
                expected: STRING_PILL_REFERENCE,
                page,
                propertyName: 'arrayNoDefaultValues',
                workflowId: workflow.workflowId,
            });
        });

        test('should reject a data pill on an object that holds an entry', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const objectProperty = getProperty(configurationPanel, 'objectNoPredefinedProperties');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await objectProperty.getByText('Object No Predefined Properties').click();

            await getVarDataPill(dataPillPanel, 'String').click();

            await expect(getPropertyMentions(objectProperty)).toHaveCount(0);
            await expect(getPropertyEditor(getProperty(objectProperty, 'key'))).toHaveText('value');

            await expectStoredParameter({
                expected: {key: 'value'},
                page,
                propertyName: 'objectNoPredefinedProperties',
                workflowId: workflow.workflowId,
            });
        });

        test('should open the data pill editor on $ from an empty array add button', async ({
            authenticatedPage: page,
        }) => {
            const arrayProperty = getProperty(configurationPanel, 'arrayNoDefaultValues');

            await arrayProperty.getByRole('button', {name: 'Add array item'}).focus();

            await page.keyboard.press('$');

            await expect(getPropertyEditor(arrayProperty)).toBeFocused();
            await expect(getDynamicSwitch(arrayProperty)).toBeChecked();
        });

        test('should switch an empty array to Formula on = from its add button', async ({authenticatedPage: page}) => {
            const arrayProperty = getProperty(configurationPanel, 'arrayNoDefaultValues');

            await arrayProperty.getByRole('button', {name: 'Add array item'}).focus();

            await page.keyboard.press('=');

            await expect(getFormulaSwitch(arrayProperty)).toBeChecked();
            await expect(getPropertyEditor(arrayProperty)).toBeFocused();
        });
    });

    test.describe('Legacy values', () => {
        test('should open a number value mixing a data pill and text in an editor without the one-pill limit', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const legacyProperty = getProperty(configurationPanel, 'numberMinNumPrecision');
            const legacyEditor = getPropertyEditor(legacyProperty);

            await expect(getFormulaSwitch(legacyProperty)).not.toBeChecked();
            await expect(legacyEditor).toHaveText('var_1.Number ms');
            await expect(getPropertyMentions(legacyProperty)).toHaveAttribute('data-id', 'var_1.Number');

            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await legacyEditor.click();
            await legacyEditor.press('End');

            const saveResponsePromise = propertyTestingParametersSavePromise(
                page,
                `${NUMBER_PILL_REFERENCE} ms${STRING_PILL_REFERENCE}`
            );

            await getVarDataPill(dataPillPanel, 'String').click();

            await saveResponsePromise;

            await expect(getPropertyMentions(legacyProperty)).toHaveCount(2);

            await expectStoredParameter({
                expected: `${NUMBER_PILL_REFERENCE} ms${STRING_PILL_REFERENCE}`,
                page,
                propertyName: 'numberMinNumPrecision',
                workflowId: workflow.workflowId,
            });
        });

        test('should take typed text in a number value mixing a data pill and text', async ({
            authenticatedPage: page,
        }) => {
            const legacyEditor = getPropertyEditor(getProperty(configurationPanel, 'numberMinNumPrecision'));

            await expect(legacyEditor).toHaveText('var_1.Number ms');

            await legacyEditor.click();
            await legacyEditor.press('End');

            await page.keyboard.type(' x');

            await expect(legacyEditor).toHaveText('var_1.Number ms x', {timeout: 5000});
        });
    });

    test.describe('Persistence', () => {
        test('should keep formula and data pill values after reopening the panel and reloading the page', async ({
            authenticatedPage: page,
        }) => {
            const dataPillPanel = await openVarDataPills(page, configurationPanel);

            await test.step('Enter a formula and a data pill', async () => {
                const numberProperty = getProperty(configurationPanel, 'numberPrecision');

                await getFormulaSwitch(numberProperty).click();

                await expect(getPropertyEditor(numberProperty)).toBeFocused();

                const formulaSaveResponsePromise = propertyTestingParametersSavePromise(page, '=1 + 2');

                await page.keyboard.type('1 + 2');

                await formulaSaveResponsePromise;

                const pillSaveResponsePromise = propertyTestingParametersSavePromise(page, NUMBER_PILL_REFERENCE);

                await getNativeInput(getProperty(configurationPanel, 'integerMaxValue')).click();
                await getVarDataPill(dataPillPanel, 'Number').click();

                await pillSaveResponsePromise;
            });

            const assertValues = async (panel: Locator) => {
                const numberProperty = getProperty(panel, 'numberPrecision');
                const integerProperty = getProperty(panel, 'integerMaxValue');

                await expect(getFormulaSwitch(numberProperty)).toBeChecked();
                await expect(getPropertyEditor(numberProperty)).toHaveText('1 + 2');
                await expect(getFormulaSwitch(integerProperty)).not.toBeChecked();
                await expect(getPropertyMentions(integerProperty)).toHaveAttribute('data-id', 'var_1.Number');
            };

            await test.step('Reopen the panel', async () => {
                await page.getByRole('button', {name: 'Close the node details dialog'}).click();

                await expect(configurationPanel).toBeHidden();

                await assertValues(await openPropertyTestingPanel(page, ANCHOR_PROPERTY_LABEL));
            });

            await test.step('Reload the page', async () => {
                await page.reload();

                await assertValues(await openPropertyTestingPanel(page, ANCHOR_PROPERTY_LABEL));
            });
        });
    });

    test.describe('Placeholder', () => {
        test('should mention formula, not expression, in the placeholders', async () => {
            const stringProperty = getProperty(configurationPanel, 'stringMinLength');

            const placeholder = getPropertyEditor(stringProperty).locator('[data-placeholder]');

            await expect(placeholder).toHaveAttribute(
                'data-placeholder',
                "Use '$' for data pills and '=' for a formula"
            );
            await expect(placeholder).not.toHaveAttribute('data-placeholder', /expression/i);
        });
    });
});
