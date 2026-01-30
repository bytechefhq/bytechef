import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import sampleWorkflow from '../../sampleWorkflow.json';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {type TestProjectI, type TestWorkflowI} from '../../utils/projectUtils';
import {getWorkflowDefinition, openPropertiesTab, reopenConfigurationPanel} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

test.describe('Reading from Workflow Definition', () => {
    let configurationPanel: Locator;

    test('should import the sample workflow and verify all properties are rendered', async ({
        authenticatedPage,
        project,
        workflow,
    }) => {
        await test.step('Navigate to workflow editor and wait for load', async () => {
            const workflowPage = new WorkflowPage(authenticatedPage);

            await workflowPage.goToWorkflowEditor(project.id, workflow.workflowId);
        });

        await test.step('Open var_1 node configuration panel', async () => {
            const varNode = authenticatedPage.getByLabel('var_1 node');

            configurationPanel = authenticatedPage.getByLabel('var_1 component configuration panel');

            await clickAndExpectToBeVisible({
                target: configurationPanel,
                trigger: varNode,
            });
        });

        await test.step('Verify all properties are rendered', async () => {
            const propertiesTabButton = configurationPanel.getByRole('button', {name: 'Properties'});

            const typeProperty = configurationPanel.getByLabel('type property');

            await clickAndExpectToBeVisible({
                target: typeProperty,
                trigger: propertiesTabButton,
            });

            const valueProperty = configurationPanel.getByLabel('value property');

            const valuePropertyList = valueProperty.getByRole('list', {name: 'value object properties'});

            await expect(valuePropertyList).toBeVisible();

            const valuePropertyItems = valuePropertyList.getByRole('listitem');

            await expect(valuePropertyItems).toHaveCount(11);
        });

        await test.step('Verify properties with default values are correctly rendered', async () => {
            const expectedValues = WorkflowPage.getExpectedPropertyValues(sampleWorkflow, 'var_1');

            await test.step('string property', async () => {
                const stringProperty = configurationPanel.getByLabel('String property');

                const stringInput = stringProperty.getByRole('textbox');

                await expect(stringInput).toHaveText(expectedValues.String);
            });

            await test.step('integer property', async () => {
                const integerProperty = configurationPanel.getByLabel('Integer property');

                const integerInput = integerProperty.getByRole('textbox');

                await expect(integerInput).toHaveValue(expectedValues.Integer);
            });

            await test.step('number property', async () => {
                const numberProperty = configurationPanel.getByLabel('Number property');

                const numberInput = numberProperty.getByRole('textbox');

                await expect(numberInput).toHaveValue(expectedValues.Number);
            });

            await test.step('boolean property', async () => {
                const booleanProperty = configurationPanel.getByLabel('Boolean property');

                const booleanSelect = booleanProperty.getByRole('combobox');

                await expect(booleanSelect).toHaveText(expectedValues.Boolean);
            });

            await test.step('date property', async () => {
                const dateProperty = configurationPanel.getByLabel('Date property');

                const dateInput = dateProperty.locator('input[type="date"]');

                await expect(dateInput).toHaveValue(expectedValues.Date);
            });

            await test.step('date time property', async () => {
                const dateTimeProperty = configurationPanel.getByLabel('DateTime property');

                const dateTimeInput = dateTimeProperty.locator('input[type="datetime-local"]');

                await expect(dateTimeInput).toHaveValue(expectedValues.DateTime);
            });

            await test.step('time property', async () => {
                const timeProperty = configurationPanel.getByLabel('Time property');

                const timeInput = timeProperty.locator('input[type="time"]');

                await expect(timeInput).toHaveValue(expectedValues.Time);
            });

            await test.step('array property', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const arrayItem = arrayProperty.getByLabel('Array property item at index 0');

                await expect(arrayItem).toBeVisible();

                const arrayItemInput = arrayItem.getByRole('textbox');

                await expect(arrayItemInput).toHaveText(expectedValues.Array);
            });
        });
    });
});

test.describe('Saving to Workflow Definition', () => {
    let configurationPanel: Locator;
    let authenticatedPage: Page;
    let project: TestProjectI;
    let workflow: TestWorkflowI;
    let expectedPropertyValue: string | number | boolean | string[] | undefined;
    let expectedPropertyName: string | undefined;
    let expectedPropertyLabel: string | undefined;
    let expectedPropertyType: string;

    test.beforeEach(async ({authenticatedPage: page, project: testProject, workflow: testWorkflow}) => {
        authenticatedPage = page;
        project = testProject;
        workflow = testWorkflow;
        expectedPropertyValue = undefined;
        expectedPropertyName = undefined;
        expectedPropertyLabel = undefined;

        await test.step('Navigate to workflow editor and wait for load', async () => {
            const workflowPage = new WorkflowPage(authenticatedPage);

            await workflowPage.goToWorkflowEditor(project.id, workflow.workflowId);
        });

        await test.step('Open var_1 node configuration panel', async () => {
            const varNode = authenticatedPage.getByLabel('var_1 node');

            configurationPanel = authenticatedPage.getByLabel('var_1 component configuration panel');

            await clickAndExpectToBeVisible({
                target: configurationPanel,
                trigger: varNode,
            });
        });

        await test.step('Switch to the Properties tab', async () => {
            const propertiesTabButton = configurationPanel.getByRole('button', {name: 'Properties'});

            const typeProperty = configurationPanel.getByLabel('type property');

            await clickAndExpectToBeVisible({
                target: typeProperty,
                trigger: propertiesTabButton,
            });
        });
    });

    test.afterEach(async () => {
        if (
            expectedPropertyValue !== undefined &&
            expectedPropertyName !== undefined &&
            expectedPropertyLabel !== undefined
        ) {
            await test.step('Verify workflow definition is updated', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                expect(workflowDefinition.tasks?.[0]?.parameters?.value?.[expectedPropertyName!]).toEqual(
                    expectedPropertyValue
                );
            });

            await test.step('Reload page and verify value persists', async () => {
                await authenticatedPage.reload();

                await authenticatedPage.waitForLoadState('domcontentloaded');

                await authenticatedPage.waitForTimeout(2000);

                configurationPanel = await reopenConfigurationPanel(authenticatedPage);

                await openPropertiesTab(configurationPanel);

                const property = configurationPanel.getByLabel(expectedPropertyLabel!, {exact: true});

                const propertyInput = property.getByRole('textbox');

                if (expectedPropertyName === 'String') {
                    await expect(propertyInput).toHaveText(expectedPropertyValue!.toString());
                } else if (expectedPropertyName === 'Boolean') {
                    const booleanCombobox = property.getByRole('combobox');

                    await expect(booleanCombobox).toHaveText(expectedPropertyValue ? 'True' : 'False');
                } else if (Array.isArray(expectedPropertyValue)) {
                    const propertyInputValue = await propertyInput.innerText();

                    expect(expectedPropertyType).toBe('object');
                    expect(propertyInputValue).toBe(expectedPropertyValue[0].toString());
                } else {
                    const propertyInputValue = await propertyInput.inputValue();

                    expect(expectedPropertyType).toBe(typeof expectedPropertyValue);
                    expect(propertyInputValue).toBe(expectedPropertyValue!.toString());
                }
            });
        }
    });

    test('should save basic STRING property', async () => {
        const stringProperty = configurationPanel.getByLabel('String property');

        const stringInput = stringProperty.getByRole('textbox');

        const testValue = 'test value';

        await stringInput.fill(testValue);

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'String';
        expectedPropertyLabel = 'String property';
    });

    test('should save basic INTEGER property', async () => {
        const integerProperty = configurationPanel.getByLabel('Integer property');

        const integerInput = integerProperty.getByRole('textbox');

        const testValue = 123;

        await integerInput.fill(testValue.toString());

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'Integer';
        expectedPropertyLabel = 'Integer property';
    });

    test('should save basic NUMBER property', async () => {
        const numberProperty = configurationPanel.getByLabel('Number property');

        const numberInput = numberProperty.getByRole('textbox');

        const testValue = 123.45;

        await numberInput.fill(testValue.toString());

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'Number';
        expectedPropertyLabel = 'Number property';
    });

    test('should save basic BOOLEAN property', async () => {
        const booleanProperty = configurationPanel.getByLabel('Boolean property');

        const booleanSelect = booleanProperty.getByLabel('Select');
        const booleanSelectOptions = authenticatedPage.getByLabel('Select options');

        await clickAndExpectToBeVisible({
            target: booleanSelectOptions,
            trigger: booleanSelect,
        });

        const testValue = false;

        await booleanSelectOptions.getByRole('option', {name: testValue ? 'True' : 'False'}).click();

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'Boolean';
        expectedPropertyLabel = 'Boolean property';
    });

    test('should save basic DATE property', async () => {
        const dateProperty = configurationPanel.getByLabel('Date property');

        const dateInput = dateProperty.getByRole('textbox');

        const testValue = '2026-01-01';

        await dateInput.fill(testValue);

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'Date';
        expectedPropertyLabel = 'Date property';
    });

    test('should save basic DATE_TIME property', async () => {
        const dateTimeProperty = configurationPanel.getByLabel('DateTime property');

        const dateTimeInput = dateTimeProperty.getByRole('textbox');

        const testValue = '2026-01-01T00:00';

        await dateTimeInput.fill(testValue);

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'DateTime';
        expectedPropertyLabel = 'DateTime property';
    });

    test('should save basic TIME property', async () => {
        const timeProperty = configurationPanel.getByLabel('Time property', {exact: true});

        const timeInput = timeProperty.getByRole('textbox');

        const testValue = '00:00';

        await timeInput.fill(testValue);

        expectedPropertyType = typeof testValue;
        expectedPropertyValue = testValue;
        expectedPropertyName = 'Time';
        expectedPropertyLabel = 'Time property';
    });

    test('should save ARRAY STRING subproperty', async () => {
        const arrayProperty = configurationPanel.getByLabel('Array property');

        const arrayItem = arrayProperty.getByLabel('Array property item at index 0');

        const arrayItemInput = arrayItem.getByRole('textbox');

        const testValue = 'test value';

        await arrayItemInput.fill(testValue);

        expectedPropertyType = 'object';
        expectedPropertyValue = [testValue];
        expectedPropertyName = 'Array';
        expectedPropertyLabel = 'Array property';
    });
});
