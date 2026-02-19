import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import sampleWorkflow from '../../sampleWorkflow.json';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {type TestProjectI, type TestWorkflowI} from '../../utils/projectUtils';
import {getWorkflowDefinition, openPropertiesTab, reopenConfigurationPanel} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

test.describe('ArrayProperty - Array property type (ArrayProperty.tsx)', () => {
    let configurationPanel: Locator;
    let authenticatedPage: Page;
    let project: TestProjectI;
    let workflow: TestWorkflowI;

    test.beforeEach(async ({authenticatedPage: page, project: testProject, workflow: testWorkflow}) => {
        authenticatedPage = page;
        project = testProject;
        workflow = testWorkflow;

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

        await test.step('Switch to the Properties tab and expand value', async () => {
            await openPropertiesTab(configurationPanel);

            const valueProperty = configurationPanel.getByLabel('value property');

            const valuePropertyList = valueProperty.getByRole('list', {name: 'value object properties'});

            await expect(valuePropertyList).toBeVisible();
        });
    });

    test.describe('Rendering from workflow definition', () => {
        test('should render array property with items from workflow definition', async () => {
            await test.step('Locate Array property section', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                await expect(arrayProperty).toBeVisible();
            });

            await test.step('Verify array items are rendered', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);

                const expectedArray = sampleWorkflow.tasks?.[0]?.parameters?.value?.Array as string[] | undefined;

                await expect(arrayItems).toHaveCount(expectedArray?.length ?? 0);
            });

            await test.step('Verify first array item value matches definition', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                const input = firstItem.getByRole('textbox');

                const expectedValue = (sampleWorkflow.tasks?.[0]?.parameters?.value?.Array as string[])?.[0];

                await expect(input).toHaveValue(expectedValue ?? '');
            });
        });

        test('should render empty array when definition has empty array', async () => {
            await test.step('Navigate to or create workflow with empty value.Array', async () => {
                // TODO: Use a workflow where value.Array is [] or ensure empty state is testable
            });

            await test.step('Verify no array items are rendered', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);

                await expect(arrayItems).toHaveCount(0);
            });

            await test.step('Verify Add array item button is visible', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addButton = arrayProperty.getByRole('button', {name: /Add array item/i});

                await expect(addButton).toBeVisible();
            });
        });

        test('should render array item with correct control type (e.g. STRING)', async () => {
            await test.step('Verify item at index 0 has textbox for STRING type', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                await expect(firstItem.getByRole('textbox')).toBeVisible();
            });
        });
    });

    test.describe('Add array item', () => {
        test('should show Add array item button when array has single item type', async () => {
            await test.step('Verify Add array item button is present', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addButton = arrayProperty.getByRole('button', {name: /Add array item/i});

                await expect(addButton).toBeVisible();
            });
        });

        test('should add new item when clicking Add array item', async () => {
            await test.step('Click Add array item', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addButton = arrayProperty.getByRole('button', {name: /Add array item/i});

                await addButton.click();
            });

            await test.step('Verify new item appears at next index', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);

                const initialCount = (sampleWorkflow.tasks?.[0]?.parameters?.value?.Array as string[])?.length ?? 0;

                await expect(arrayItems).toHaveCount(initialCount + 1);
            });
        });

        test('should persist new array item to workflow definition after add', async () => {
            await test.step('Add array item and wait for debounced save', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addButton = arrayProperty.getByRole('button', {name: /Add array item/i});

                await addButton.click();

                await authenticatedPage.waitForTimeout(700);
            });

            await test.step('Verify workflow definition contains updated array', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = workflowDefinition.tasks?.[0]?.parameters?.value?.Array;

                expect(Array.isArray(arrayValue)).toBe(true);
                // TODO: Assert expected length or structure
            });
        });

        test('should show SubPropertyPopover when array has multiple item types', async () => {
            await test.step('Use workflow with array of multiple types (e.g. condition) if available', async () => {
                // TODO: When availablePropertyTypes.length > 1, SubPropertyPopover is shown instead of plain button
            });

            await test.step('Verify type selector and add behavior', async () => {
                // TODO: Select type from popover, then add item
            });
        });
    });

    test.describe('Delete array item', () => {
        test('should show delete button on custom array items', async () => {
            await test.step('Add an item so the new item has custom: true and shows delete button', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                await arrayProperty.getByRole('button', {name: /Add array item/i}).click();
            });

            await test.step('Verify delete button is present on the added (custom) item', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addedItem = arrayProperty.getByLabel('Array property item at index 1');

                const deleteButton = addedItem.getByRole('button');

                await expect(deleteButton).toBeVisible();
            });
        });

        test('should remove item from list when delete is clicked', async () => {
            await test.step('Add second item then delete it', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                await arrayProperty.getByRole('button', {name: /Add array item/i}).click();

                const addedItem = arrayProperty.getByLabel('Array property item at index 1');

                await addedItem.getByRole('button').click();
            });

            await test.step('Verify item count decreased back to 1', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const arrayItems = arrayProperty.getByLabel(/Array property item at index \d+/);

                await expect(arrayItems).toHaveCount(1);
            });
        });

        test('should persist array after delete to workflow definition', async () => {
            await test.step('Add item then delete it and wait for debounced save', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                await arrayProperty.getByRole('button', {name: /Add array item/i}).click();

                await authenticatedPage.waitForTimeout(300);

                const addedItem = arrayProperty.getByLabel('Array property item at index 1');

                await addedItem.getByRole('button').click();

                await authenticatedPage.waitForTimeout(700);
            });

            await test.step('Verify workflow definition array has one item again', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = workflowDefinition.tasks?.[0]?.parameters?.value?.Array;

                expect(Array.isArray(arrayValue)).toBe(true);
                expect(arrayValue).toHaveLength(1);
            });
        });
    });

    test.describe('Edit array item value', () => {
        test('should allow editing value in array item', async () => {
            await test.step('Change value in first array item', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                const input = firstItem.getByRole('textbox');

                await input.fill('edited array value');
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(700);
            });
        });

        test('should persist edited array item value to workflow definition', async () => {
            const newValue = 'persisted array value';

            await test.step('Edit first array item', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                const input = firstItem.getByRole('textbox');

                await input.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(700);
            });

            await test.step('Verify workflow definition has new value', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = workflowDefinition.tasks?.[0]?.parameters?.value?.Array as string[];

                expect(arrayValue?.[0]).toBe(newValue);
            });
        });

        test('should persist after reload when array item value was edited', async () => {
            const newValue = 'reload persisted value';

            await test.step('Edit first array item', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                const input = firstItem.getByRole('textbox');

                await input.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(700);
            });

            await test.step('Reload and verify value persists', async () => {
                await authenticatedPage.reload();

                await authenticatedPage.waitForLoadState('domcontentloaded');

                await authenticatedPage.waitForTimeout(2000);

                configurationPanel = await reopenConfigurationPanel(authenticatedPage);

                await openPropertiesTab(configurationPanel);

                const arrayProperty = configurationPanel.getByLabel('Array property');

                const firstItem = arrayProperty.getByLabel('Array property item at index 0');

                const input = firstItem.getByRole('textbox');

                await expect(input).toHaveValue(newValue);
            });
        });
    });

    test.describe('Persistence and path encoding', () => {
        test('should save array with correct path (e.g. value.Array)', async () => {
            await test.step('Edit array item and verify path in definition', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                expect(workflowDefinition.tasks?.[0]?.parameters?.value).toHaveProperty('Array');
            });
        });

        test('should handle value.Array[0], value.Array[1] paths in metadata when present', async () => {
            await test.step('Verify dynamicPropertyTypes or parameter paths if applicable', async () => {
                // TODO: Check metadata.ui.dynamicPropertyTypes for value.Array[0] etc.
            });
        });
    });

    test.describe('Edge cases and multiple items', () => {
        test('should render multiple array items with correct indices', async () => {
            await test.step('Add second item so array has at least 2 items', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const addButton = arrayProperty.getByRole('button', {name: /Add array item/i});

                await addButton.click();
            });

            await test.step('Verify both items visible with indices 0 and 1', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                await expect(arrayProperty.getByLabel('Array property item at index 0')).toBeVisible();
                await expect(arrayProperty.getByLabel('Array property item at index 1')).toBeVisible();
            });
        });

        test('should allow different values in each array item', async () => {
            await test.step('Set different values in index 0 and index 1', async () => {
                const arrayProperty = configurationPanel.getByLabel('Array property');

                const item0 = arrayProperty.getByLabel('Array property item at index 0');
                const item1 = arrayProperty.getByLabel('Array property item at index 1');

                await item0.getByRole('textbox').fill('first');
                await item1.getByRole('textbox').fill('second');
            });

            await test.step('Wait for debounced save and verify definition', async () => {
                await authenticatedPage.waitForTimeout(700);

                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = workflowDefinition.tasks?.[0]?.parameters?.value?.Array as string[];

                expect(arrayValue?.[0]).toBe('first');
                expect(arrayValue?.[1]).toBe('second');
            });
        });

        test('should handle array of OBJECT items when workflow has such structure', async () => {
            await test.step('Use or create workflow with value.Array as array of objects', async () => {
                // TODO: ArrayProperty supports items[0].type === 'OBJECT' and maps parameterValue to subProperties
            });

            await test.step('Verify object items render with nested properties', async () => {
                // TODO: Assert nested property inputs for each array element
            });
        });
    });
});
