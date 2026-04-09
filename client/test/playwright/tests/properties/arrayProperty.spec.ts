import {type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import sampleWorkflow from '../../sampleWorkflow.json';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {type TestProjectI, type TestWorkflowI} from '../../utils/projectUtils';
import {
    type WorkflowDefinitionI,
    addArrayItemViaPopover,
    getPropertyValue,
    getWorkflowDefinition,
    openPropertiesTab,
    reopenConfigurationPanel,
    replaceMentionsInputValue,
} from '../../utils/workflowUtils';

const SAMPLE_VAR_ARRAY = getPropertyValue({
    propertyName: 'Array',
    taskName: 'var_1',
    workflowDefinition: sampleWorkflow as WorkflowDefinitionI,
}) as string[] | undefined;

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

test.describe('ArrayProperty - Array property type (ArrayProperty.tsx)', () => {
    let authenticatedPage: Page;
    let project: TestProjectI;
    let workflow: TestWorkflowI;
    let workflowPage: WorkflowPage;

    test.beforeEach(async ({authenticatedPage: page, project: testProject, workflow: testWorkflow}) => {
        authenticatedPage = page;
        project = testProject;
        workflow = testWorkflow;

        workflowPage = new WorkflowPage(authenticatedPage);

        await test.step('Navigate to workflow editor and wait for load', async () => {
            await workflowPage.goToWorkflowEditor(project.id, workflow.workflowId);
        });

        await test.step('Open var_1 node configuration panel', async () => {
            await clickAndExpectToBeVisible({
                target: workflowPage.firstTaskComponentConfigurationPanel,
                trigger: workflowPage.firstNode,
            });
        });

        await test.step('Switch to the Properties tab and expand value', async () => {
            await openPropertiesTab(workflowPage.firstTaskComponentConfigurationPanel);

            const valuePropertyList = workflowPage.parentObjectProperty.getByRole('list', {
                name: 'value object properties',
            });

            await expect(valuePropertyList).toBeVisible();
        });
    });

    test.describe('Rendering from workflow definition', () => {
        test('should render array property with items from workflow definition', async () => {
            await test.step('Locate Array property section', async () => {
                await expect(workflowPage.arrayProperty).toBeVisible();
            });

            await test.step('Verify array items are rendered', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                const expectedArray = SAMPLE_VAR_ARRAY;

                await expect(arrayItems).toHaveCount(expectedArray?.length ?? 0);
            });

            await test.step('Verify first array item value matches definition', async () => {
                const input = workflowPage.arrayPropertyItemTextboxAt(0);

                const expectedValue = SAMPLE_VAR_ARRAY?.[0];

                await expect(input).toContainText(expectedValue ?? '');
            });
        });

        test('should render array item with correct control type (e.g. STRING)', async () => {
            await test.step('Verify item at index 0 has textbox for STRING type', async () => {
                const arrayPropertyItemTextboxAtIndex0 = workflowPage.arrayPropertyItemTextboxAt(0);

                await expect(arrayPropertyItemTextboxAtIndex0).toBeVisible();
            });
        });
    });

    test.describe('Add array item', () => {
        test('should show Add array item button when array has single item type', async () => {
            const addButton = workflowPage.arrayProperty.getByRole('button', {name: /Add array item/i});

            await expect(addButton).toBeVisible();
        });

        test('should add new item when clicking Add array item', async () => {
            const initialCount = SAMPLE_VAR_ARRAY?.length ?? 0;

            await test.step('Add an array item', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    page: authenticatedPage,
                });
            });

            await test.step('Verify new item appears at next index', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(initialCount + 1);
            });
        });

        test('should persist new array item to workflow definition after add', async () => {
            await test.step('Add array item and wait for debounced save', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    page: authenticatedPage,
                });

                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Verify workflow definition contains updated array', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = WorkflowPage.getFirstTaskArrayParameter(workflowDefinition);

                WorkflowPage.assertVar1ArrayParameterIsDefined(arrayValue);
            });
        });

        test('should show SubPropertyPopover when array has multiple item types', async () => {
            await test.step('Click Add array item and verify popover with type selector appears', async () => {
                const addButton = workflowPage.arrayProperty.getByRole('button', {name: /Add array item/i});
                const popover = authenticatedPage.getByLabel('Array property popover');

                await clickAndExpectToBeVisible({
                    target: popover,
                    trigger: addButton,
                });

                const typeCombobox = popover.getByRole('combobox');

                await expect(typeCombobox).toBeVisible();

                await authenticatedPage.keyboard.press('Escape');

                await expect(popover).not.toBeVisible();
            });

            await test.step('Select a different type and add item', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    itemType: 'INTEGER',
                    page: authenticatedPage,
                });

                const arrayItems = workflowPage.arrayPropertyItems;

                const expectedInitialCount = SAMPLE_VAR_ARRAY?.length ?? 0;

                await expect(arrayItems).toHaveCount(expectedInitialCount + 1);
            });
        });
    });

    test.describe('Delete array item', () => {
        test('should show delete button on custom array items', async () => {
            await test.step('Add an item so the new item has custom: true and shows delete button', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    page: authenticatedPage,
                });
            });

            await test.step('Verify delete button is present on the added item', async () => {
                const addedItem = workflowPage.arrayPropertyItemAt(0);

                const deleteButton = addedItem.getByRole('button');

                await expect(deleteButton).toBeVisible();
            });
        });

        test('should remove item from list when delete is clicked', async () => {
            await test.step('Add two items', async () => {
                await workflowPage.addArrayItemsToReachRowCount({
                    targetRowCount: 2,
                });

                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(2);
            });

            await test.step('Delete the first item', async () => {
                const firstItem = workflowPage.arrayPropertyItemAt(0);

                const deleteButton = firstItem.getByRole('button');

                await deleteButton.click();
            });

            await test.step('Verify item count decreased back to 1', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(1);
            });
        });

        test('should persist array after delete to workflow definition', async () => {
            await test.step('Add item then delete it and wait for debounced save', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    page: authenticatedPage,
                });

                await authenticatedPage.waitForTimeout(WorkflowPage.SHORT_DEBOUNCE_MS);

                const addedItem = workflowPage.arrayPropertyItemAt(1);

                await addedItem.getByRole('button').click();

                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Verify workflow definition array has one item again', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = WorkflowPage.getFirstTaskArrayParameter(workflowDefinition);

                WorkflowPage.assertVar1ArrayParameterIsDefined(arrayValue);
                expect(arrayValue).toHaveLength(1);
            });
        });

        test('should not show the deleted row value on the next row when the next row was empty', async () => {
            const valueOnDeletedRow = 'ByteChef';

            await test.step('Add a second row; first row has a value, second row stays empty', async () => {
                await addArrayItemViaPopover({
                    arrayProperty: workflowPage.arrayProperty,
                    page: authenticatedPage,
                });

                await authenticatedPage.waitForTimeout(WorkflowPage.SHORT_DEBOUNCE_MS);

                const firstRowInput = workflowPage.arrayPropertyItemTextboxAt(0);

                await firstRowInput.fill(valueOnDeletedRow);

                const secondRowInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await expect(secondRowInput).toHaveText('');
            });

            await test.step('Delete the first row (the row that held the value)', async () => {
                const firstRow = workflowPage.arrayPropertyItemAt(0);

                await firstRow.getByRole('button').click();
            });

            await test.step('The remaining row must stay empty (value must not jump from the deleted row)', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(1);

                const remainingRowInput = workflowPage.arrayPropertyItemTextboxAt(0);

                await expect(remainingRowInput).toBeEmpty();
            });
        });

        test('should not show the deleted middle row value on another row when neighbors were empty', async () => {
            const valueOnMiddleRow = 'ByteChef';

            await test.step('Add two rows so there are three rows total', async () => {
                await workflowPage.addArrayItemsToReachRowCount({
                    targetRowCount: 3,
                });

                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(3);
            });

            await test.step('Clear first row, set middle row value, leave last row empty', async () => {
                const firstRowInput = workflowPage.arrayPropertyItemTextboxAt(0);

                await firstRowInput.fill('');

                const middleRowInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await middleRowInput.fill(valueOnMiddleRow);

                const lastRowInput = workflowPage.arrayPropertyItemTextboxAt(2);

                await expect(lastRowInput).toHaveText('');
            });

            await test.step('Delete the middle row', async () => {
                const middleRow = workflowPage.arrayPropertyItemAt(1);

                await middleRow.getByRole('button').click();
            });

            await test.step('Neither remaining row should display the deleted middle value', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(2);

                const firstRemainingInput = workflowPage.arrayPropertyItemTextboxAt(0);

                const secondRemainingInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await expect(firstRemainingInput).not.toContainText(valueOnMiddleRow);
                await expect(secondRemainingInput).not.toContainText(valueOnMiddleRow);
            });
        });

        test('should show foo and bar in order after deleting the empty middle item', async () => {
            const firstRowValue = 'foo';
            const thirdRowValue = 'bar';

            await test.step('Add two rows so there are three rows total', async () => {
                await workflowPage.addArrayItemsToReachRowCount({
                    targetRowCount: 3,
                });

                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(3);
            });

            await test.step('Set first row to foo, leave second row empty, set third row to bar', async () => {
                const firstRowInput = workflowPage.arrayPropertyItemTextboxAt(0);

                await replaceMentionsInputValue({
                    input: firstRowInput,
                    page: authenticatedPage,
                    value: firstRowValue,
                });

                const middleRowInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await replaceMentionsInputValue({
                    input: middleRowInput,
                    page: authenticatedPage,
                    value: '',
                });

                const thirdRowInput = workflowPage.arrayPropertyItemTextboxAt(2);

                await replaceMentionsInputValue({
                    input: thirdRowInput,
                    page: authenticatedPage,
                    value: thirdRowValue,
                });

                await expect(middleRowInput).toBeEmpty();

                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Delete the middle row (the one with no value)', async () => {
                const middleRow = workflowPage.arrayPropertyItemAt(1);

                await middleRow.getByRole('button').click();
            });

            await test.step('UI shows foo then bar on the two remaining rows', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(2);

                const firstRemainingInput = workflowPage.arrayPropertyItemTextboxAt(0);

                const secondRemainingInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await expect(firstRemainingInput).toHaveText(firstRowValue);
                await expect(secondRemainingInput).toHaveText(thirdRowValue);
            });
        });

        test('should keep the following INTEGER row empty after deleting a filled INTEGER row above it', async () => {
            const valueOnDeletedIntegerRow = '1337';

            await test.step('Add two INTEGER rows (STRING, INTEGER, INTEGER)', async () => {
                await workflowPage.addArrayItemsToReachRowCount({
                    itemType: 'INTEGER',
                    targetRowCount: 3,
                });

                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(3);
            });

            await test.step('Set first INTEGER row to a value and leave the second INTEGER row empty', async () => {
                const firstIntegerInput = workflowPage.arrayPropertyItemTextboxAt(1);

                await replaceMentionsInputValue({
                    input: firstIntegerInput,
                    page: authenticatedPage,
                    value: valueOnDeletedIntegerRow,
                });

                const secondIntegerInput = workflowPage.arrayPropertyItemTextboxAt(2);

                await replaceMentionsInputValue({
                    input: secondIntegerInput,
                    page: authenticatedPage,
                    value: '',
                });

                await expect(secondIntegerInput).toHaveValue('');

                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Delete the INTEGER row that holds the value', async () => {
                const integerRowWithValue = workflowPage.arrayPropertyItemAt(1);

                await integerRowWithValue.getByRole('button').last().click();
            });

            await test.step('The INTEGER row that was empty after it must stay empty', async () => {
                const arrayItems = workflowPage.arrayPropertyItems;

                await expect(arrayItems).toHaveCount(2);

                const remainingIntegerRowAfterDeleted = workflowPage.arrayPropertyItemTextboxAt(1);

                await expect(remainingIntegerRowAfterDeleted).toHaveValue('');
            });
        });
    });

    test.describe('Edit array item value', () => {
        test('should allow editing value in array item', async () => {
            await test.step('Change value in first array item', async () => {
                const firstArrayItemTextbox = workflowPage.arrayPropertyItemTextboxAt(0);

                await firstArrayItemTextbox.fill('edited array value');
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });
        });

        test('should persist edited array item value to workflow definition', async () => {
            const newValue = 'persisted array value';

            await test.step('Edit first array item', async () => {
                const firstArrayItemTextbox = workflowPage.arrayPropertyItemTextboxAt(0);

                await firstArrayItemTextbox.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Verify workflow definition has new value', async () => {
                const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

                const arrayValue = WorkflowPage.getFirstTaskArrayParameter(workflowDefinition);

                WorkflowPage.assertVar1ArrayParameterIsDefined(arrayValue);
                expect((arrayValue as string[])[0]).toBe(newValue);
            });
        });

        test('should persist after reload when array item value was edited', async () => {
            const newValue = 'reload persisted value';

            await test.step('Edit first array item', async () => {
                const firstArrayItemTextbox = workflowPage.arrayPropertyItemTextboxAt(0);

                await firstArrayItemTextbox.fill(newValue);
            });

            await test.step('Wait for debounced save', async () => {
                await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);
            });

            await test.step('Reload and verify value persists', async () => {
                await authenticatedPage.reload();
                await authenticatedPage.waitForLoadState('domcontentloaded');
                await authenticatedPage.waitForTimeout(2000);

                await reopenConfigurationPanel(authenticatedPage);

                await openPropertiesTab(workflowPage.firstTaskComponentConfigurationPanel);

                const firstArrayItemTextbox = workflowPage.arrayPropertyItemTextboxAt(0);

                await expect(firstArrayItemTextbox).toContainText(newValue);
            });
        });
    });
});
