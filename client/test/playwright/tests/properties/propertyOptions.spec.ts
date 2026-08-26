import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {
    DEPENDENCY_MISSING_PLACEHOLDER,
    OPTIONS_LOOKUP_LABEL,
    OPTIONS_LOOKUP_ROUTE,
    OPTIONS_MULTISELECT_LABEL,
    OPTIONS_SELECT_LABEL,
    OPTION_LABELS,
    RESET_OPTION_LABEL,
    assertComboBoxValue,
    getComboBoxTrigger,
    getMultiSelectTrigger,
    getSelectedMultiSelectBadge,
    openComboBox,
    openMultiSelect,
    optionsRequestPromise,
    selectComboBoxOption,
    setOptionsLookupDependency,
    toggleMultiSelectOptions,
} from '../../utils/optionsUtils';
import {type TestWorkflowI} from '../../utils/projectUtils';
import {
    openPropertyTestingPanel,
    openPropertyTestingPanelAndPropertiesTab,
    propertyTestingParametersSavePromise,
} from '../../utils/propertyValidationUtils';
import {getTaskParameters, getWorkflowDefinition} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

const PROPERTY_TESTING_TASK_NAME = 'propertyTesting_1';

const LOOKUP_DEPENDENCY_VALUE = 'lookup value';

test.describe('PropertyComboBox - static options (PropertyComboBox.tsx)', () => {
    let authenticatedPage: Page;
    let configurationPanel: Locator;
    let workflow: TestWorkflowI;

    test.beforeEach(async ({authenticatedPage: page, project, workflow: testWorkflow}) => {
        authenticatedPage = page;
        workflow = testWorkflow;

        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            testWorkflow.workflowId,
            OPTIONS_SELECT_LABEL
        );
    });

    test.describe('Rendering', () => {
        test('should render an enabled combobox with the default placeholder and no selected option', async () => {
            const comboBoxTrigger = getComboBoxTrigger(configurationPanel, OPTIONS_SELECT_LABEL);

            await expect(comboBoxTrigger).toBeVisible();

            await expect(comboBoxTrigger).toBeEnabled();

            await expect(comboBoxTrigger).toHaveText('Select...');
        });

        test('should list every static option once the combobox is opened', async () => {
            await openComboBox({configurationPanel, page: authenticatedPage, propertyLabel: OPTIONS_SELECT_LABEL});

            for (const optionLabel of OPTION_LABELS) {
                await expect(authenticatedPage.getByRole('option', {exact: true, name: optionLabel})).toBeVisible();
            }
        });
    });

    test.describe('Searching', () => {
        test('should narrow the option list down to the searched option', async () => {
            await openComboBox({configurationPanel, page: authenticatedPage, propertyLabel: OPTIONS_SELECT_LABEL});

            await authenticatedPage.getByPlaceholder('Search...').fill('option3');

            await expect(authenticatedPage.getByRole('option', {exact: true, name: 'option3'})).toBeVisible();

            await expect(authenticatedPage.getByRole('option', {exact: true, name: 'option1'})).toHaveCount(0);
        });

        test('should render the empty state when the search matches no option', async () => {
            await openComboBox({configurationPanel, page: authenticatedPage, propertyLabel: OPTIONS_SELECT_LABEL});

            await authenticatedPage.getByPlaceholder('Search...').fill('no such option');

            await expect(authenticatedPage.getByText('No item found.')).toBeVisible();
        });
    });

    test.describe('Selecting and persistence', () => {
        test('should show the selected option label on the trigger and persist its value', async () => {
            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option2',
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await assertComboBoxValue(configurationPanel, OPTIONS_SELECT_LABEL, 'option2');

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsNoMultiselect).toBe('2');
        });

        test('should replace the selected option when another option is picked', async () => {
            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option2',
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option4',
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await assertComboBoxValue(configurationPanel, OPTIONS_SELECT_LABEL, 'option4');

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsNoMultiselect).toBe('4');
        });

        test('should clear the selection through the reset option', async () => {
            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option2',
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await selectComboBoxOption({
                configurationPanel,
                optionLabel: RESET_OPTION_LABEL,
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await assertComboBoxValue(configurationPanel, OPTIONS_SELECT_LABEL, 'Select...');
        });

        test('should keep the selected option after a reload', async () => {
            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option3',
                page: authenticatedPage,
                propertyLabel: OPTIONS_SELECT_LABEL,
            });

            await authenticatedPage.reload();

            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);

            configurationPanel = await openPropertyTestingPanel(authenticatedPage, OPTIONS_SELECT_LABEL);

            await assertComboBoxValue(configurationPanel, OPTIONS_SELECT_LABEL, 'option3');
        });
    });
});

test.describe('PropertyMultiSelect - static options (PropertyMultiSelect.tsx)', () => {
    let authenticatedPage: Page;
    let configurationPanel: Locator;
    let workflow: TestWorkflowI;

    test.beforeEach(async ({authenticatedPage: page, project, workflow: testWorkflow}) => {
        authenticatedPage = page;
        workflow = testWorkflow;

        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            testWorkflow.workflowId,
            OPTIONS_MULTISELECT_LABEL
        );
    });

    test.describe('Rendering', () => {
        test('should render an enabled multiselect with no selected option', async () => {
            const multiSelectTrigger = getMultiSelectTrigger(configurationPanel);

            await expect(multiSelectTrigger).toBeVisible();

            await expect(multiSelectTrigger).toBeEnabled();

            await expect(multiSelectTrigger).toHaveText('Select...');
        });

        test('should list every static option and the select all entry once opened', async () => {
            await openMultiSelect({configurationPanel, page: authenticatedPage});

            for (const optionLabel of OPTION_LABELS) {
                await expect(authenticatedPage.getByRole('option', {exact: true, name: optionLabel})).toBeVisible();
            }
        });
    });

    test.describe('Selecting and persistence', () => {
        test('should render a badge per selected option and persist them as an array', async () => {
            await toggleMultiSelectOptions({
                configurationPanel,
                optionLabels: ['option1', 'option3'],
                page: authenticatedPage,
            });

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option1')).toBeVisible();

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option3')).toBeVisible();

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsMultiselect).toEqual(['1', '3']);
        });

        test('should remove a single selected option through its badge', async () => {
            await toggleMultiSelectOptions({
                configurationPanel,
                optionLabels: ['option1', 'option3'],
                page: authenticatedPage,
            });

            const saveResponsePromise = propertyTestingParametersSavePromise(authenticatedPage);

            await configurationPanel
                .getByLabel(OPTIONS_MULTISELECT_LABEL, {exact: true})
                .getByLabel('remove-option')
                .first()
                .click();

            await saveResponsePromise;

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option1')).toHaveCount(0);

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option3')).toBeVisible();

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsMultiselect).toEqual(['3']);
        });

        test('should select every option through the select all entry', async () => {
            await openMultiSelect({configurationPanel, page: authenticatedPage});

            const saveResponsePromise = propertyTestingParametersSavePromise(authenticatedPage);

            await authenticatedPage.getByRole('option', {name: 'Select All'}).click();

            await saveResponsePromise;

            await authenticatedPage.keyboard.press('Escape');

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsMultiselect).toEqual(['1', '2', '3', '4']);
        });

        test('should clear every selected option through the clear all button', async () => {
            await toggleMultiSelectOptions({
                configurationPanel,
                optionLabels: ['option1', 'option2'],
                page: authenticatedPage,
            });

            const saveResponsePromise = propertyTestingParametersSavePromise(authenticatedPage);

            await configurationPanel
                .getByLabel(OPTIONS_MULTISELECT_LABEL, {exact: true})
                .getByLabel('clear-all')
                .click();

            await saveResponsePromise;

            await expect(getMultiSelectTrigger(configurationPanel)).toHaveText('Select...');

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            // Clearing every option drops the parameter instead of storing an empty array.
            expect(taskParameters?.optionsMultiselect).toBeUndefined();
        });

        test('should drop the options beyond the badge limit through the more badge', async () => {
            await openMultiSelect({configurationPanel, page: authenticatedPage});

            const selectAllSavePromise = propertyTestingParametersSavePromise(authenticatedPage);

            await authenticatedPage.getByRole('option', {name: 'Select All'}).click();

            await selectAllSavePromise;

            await authenticatedPage.keyboard.press('Escape');

            const multiSelectProperty = configurationPanel.getByLabel(OPTIONS_MULTISELECT_LABEL, {exact: true});

            await expect(multiSelectProperty.getByLabel('more-count')).toBeVisible();

            const clearExtraOptionsSavePromise = propertyTestingParametersSavePromise(authenticatedPage);

            await multiSelectProperty.getByLabel('clear-extra-options').click();

            await clearExtraOptionsSavePromise;

            await expect(multiSelectProperty.getByLabel('more-count')).toHaveCount(0);

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.optionsMultiselect).toEqual(['1', '2', '3']);
        });

        test('should keep the selected options after a reload', async () => {
            await toggleMultiSelectOptions({
                configurationPanel,
                optionLabels: ['option2', 'option4'],
                page: authenticatedPage,
            });

            await authenticatedPage.reload();

            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);

            configurationPanel = await openPropertyTestingPanel(authenticatedPage, OPTIONS_MULTISELECT_LABEL);

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option2')).toBeVisible();

            await expect(getSelectedMultiSelectBadge(configurationPanel, 'option4')).toBeVisible();
        });
    });
});

test.describe('PropertyComboBox - options lookup dependency (optionsLookupDependsOn)', () => {
    let authenticatedPage: Page;
    let configurationPanel: Locator;
    let workflow: TestWorkflowI;

    test.beforeEach(async ({authenticatedPage: page, project, workflow: testWorkflow}) => {
        authenticatedPage = page;
        workflow = testWorkflow;

        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            testWorkflow.workflowId,
            OPTIONS_LOOKUP_LABEL
        );
    });

    test.describe('Unresolved lookup dependency', () => {
        test('should render a disabled combobox stating that the dependency is not defined', async () => {
            const comboBoxTrigger = getComboBoxTrigger(configurationPanel, OPTIONS_LOOKUP_LABEL);

            await expect(comboBoxTrigger).toHaveText(DEPENDENCY_MISSING_PLACEHOLDER);

            await expect(comboBoxTrigger).toBeDisabled();
        });

        test('should not issue an options request while the lookup dependency is empty', async () => {
            let optionsRequested = false;

            await authenticatedPage.route(OPTIONS_LOOKUP_ROUTE, async (route) => {
                optionsRequested = true;

                await route.continue();
            });

            await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);

            expect(optionsRequested).toBe(false);
        });
    });

    test.describe('Resolved lookup dependency', () => {
        test('should enable the combobox and load the options once the dependency is set', async () => {
            await setOptionsLookupDependency({
                configurationPanel,
                page: authenticatedPage,
                value: LOOKUP_DEPENDENCY_VALUE,
            });

            const comboBoxTrigger = getComboBoxTrigger(configurationPanel, OPTIONS_LOOKUP_LABEL);

            await expect(comboBoxTrigger).toBeEnabled();

            await openComboBox({configurationPanel, page: authenticatedPage, propertyLabel: OPTIONS_LOOKUP_LABEL});

            for (const optionLabel of OPTION_LABELS) {
                await expect(authenticatedPage.getByRole('option', {exact: true, name: optionLabel})).toBeVisible();
            }
        });

        test('should re-request the options when the lookup dependency changes', async () => {
            await setOptionsLookupDependency({
                configurationPanel,
                page: authenticatedPage,
                value: LOOKUP_DEPENDENCY_VALUE,
            });

            const secondRequestPromise = optionsRequestPromise(authenticatedPage);

            await setOptionsLookupDependency({
                configurationPanel,
                page: authenticatedPage,
                value: 'changed lookup value',
                waitForRequest: false,
            });

            await secondRequestPromise;

            await expect(getComboBoxTrigger(configurationPanel, OPTIONS_LOOKUP_LABEL)).toBeEnabled();
        });

        test('should persist an option selected from the dependent combobox', async () => {
            await setOptionsLookupDependency({
                configurationPanel,
                page: authenticatedPage,
                value: LOOKUP_DEPENDENCY_VALUE,
            });

            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option2',
                page: authenticatedPage,
                propertyLabel: OPTIONS_LOOKUP_LABEL,
            });

            await assertComboBoxValue(configurationPanel, OPTIONS_LOOKUP_LABEL, 'option2');

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.setForOptionsLookup).toBe(LOOKUP_DEPENDENCY_VALUE);
            expect(taskParameters?.optionsLookupDependsOn).toBe('2');
        });

        test('should keep the selected option after a reload', async () => {
            await setOptionsLookupDependency({
                configurationPanel,
                page: authenticatedPage,
                value: LOOKUP_DEPENDENCY_VALUE,
            });

            await selectComboBoxOption({
                configurationPanel,
                optionLabel: 'option1',
                page: authenticatedPage,
                propertyLabel: OPTIONS_LOOKUP_LABEL,
            });

            await authenticatedPage.reload();

            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);

            configurationPanel = await openPropertyTestingPanel(authenticatedPage, OPTIONS_LOOKUP_LABEL);

            await assertComboBoxValue(configurationPanel, OPTIONS_LOOKUP_LABEL, 'option1');
        });
    });
});
