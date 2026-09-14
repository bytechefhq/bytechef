import {type Locator, expect, mergeTests} from '@playwright/test';

import {importSelectableIndexWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {
    applyTypedArrayIndex,
    arrayIndexTriggerName,
    getArrayIndexPopover,
    getArrayIndexTriggers,
    getMentionsInput,
    insertArrayItemDataPill,
    openArrayIndexPopover,
    pickArrayIndex,
} from '../../utils/dataPillArrayIndexUtils';
import {openPropertyTestingPanel, openPropertyTestingPanelAndPropertiesTab} from '../../utils/propertyValidationUtils';
import {getTaskParameters, getWorkflowDefinition} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importSelectableIndexWorkflowTest);

const PROPERTY_TESTING_TASK_NAME = 'propertyTesting_1';

const PROPERTY_LABEL = 'stringMinLength property';

const PROPERTY_NAME = 'stringMinLength';

const MAX_LISTED_SAMPLE_ROWS = 50;

const FRUITS = ['apple', 'banana', 'cherry'];

const MANY_ITEM_COUNT = 51;

test.describe('Data pill array index', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            PROPERTY_LABEL
        );
    });

    test.describe('Picking an item from the sample list', () => {
        test('should resolve the array index placeholder to zero when the data pill is inserted', async ({
            authenticatedPage: page,
        }) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'fruits',
                configurationPanel,
                expectedReference: 'var_1.fruits[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const mentionsInput = getMentionsInput(configurationPanel, PROPERTY_LABEL);

            await expect(mentionsInput).toHaveText('var_1.fruits[0]');
            await expect(mentionsInput).not.toContainText('[index]');

            await expect(getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL)).toHaveCount(1);
        });

        test('should list the sample items with the current index selected', async ({authenticatedPage: page}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'fruits',
                configurationPanel,
                expectedReference: 'var_1.fruits[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const arrayIndexPopover = await openArrayIndexPopover({
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                page,
            });

            await expect(arrayIndexPopover.getByText('Pick an item')).toBeVisible();
            await expect(arrayIndexPopover.getByText(`${FRUITS.length} items`)).toBeVisible();

            const sampleItems = arrayIndexPopover.getByRole('option');

            await expect(sampleItems).toHaveCount(FRUITS.length);

            for (const [fruitIndex, fruit] of FRUITS.entries()) {
                await expect(sampleItems.nth(fruitIndex)).toHaveText(`${fruitIndex}"${fruit}"`);
            }

            await expect(sampleItems.first()).toHaveAttribute('aria-selected', 'true');
            await expect(sampleItems.nth(1)).toHaveAttribute('aria-selected', 'false');
        });

        test('should update the pill and persist the reference when an item is picked', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'fruits',
                configurationPanel,
                expectedReference: 'var_1.fruits[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            await pickArrayIndex({
                arrayIndex: 2,
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                expectedReference: 'var_1.fruits[2]',
                page,
            });

            await expect(getArrayIndexPopover(page)).toBeHidden();

            await expect(getMentionsInput(configurationPanel, PROPERTY_LABEL)).toHaveText('var_1.fruits[2]');

            await expect(configurationPanel.getByRole('button', {name: arrayIndexTriggerName(2)})).toBeVisible();

            const workflowDefinition = await getWorkflowDefinition(page, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.[PROPERTY_NAME]).toBe('${var_1.fruits[2]}');
        });
    });

    test.describe('Nested arrays', () => {
        test('should render one index picker per array segment', async ({authenticatedPage: page}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'tags',
                configurationPanel,
                expectedReference: 'var_1.orders[0].tags[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const mentionsInput = getMentionsInput(configurationPanel, PROPERTY_LABEL);

            await expect(mentionsInput).toHaveText('var_1.orders[0].tags[0]');
            await expect(mentionsInput).not.toContainText('[index]');

            await expect(getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL)).toHaveCount(2);
        });

        test('should change each array index independently', async ({authenticatedPage: page, workflow}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'tags',
                configurationPanel,
                expectedReference: 'var_1.orders[0].tags[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            await test.step('Pick the second tag of the first order', async () => {
                await pickArrayIndex({
                    arrayIndex: 1,
                    arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).nth(1),
                    expectedReference: 'var_1.orders[0].tags[1]',
                    page,
                });

                await expect(getMentionsInput(configurationPanel, PROPERTY_LABEL)).toHaveText(
                    'var_1.orders[0].tags[1]'
                );
            });

            await test.step('Pick the second order and keep the tag index', async () => {
                await pickArrayIndex({
                    arrayIndex: 1,
                    arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                    expectedReference: 'var_1.orders[1].tags[1]',
                    page,
                });

                await expect(getMentionsInput(configurationPanel, PROPERTY_LABEL)).toHaveText(
                    'var_1.orders[1].tags[1]'
                );
            });

            const workflowDefinition = await getWorkflowDefinition(page, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.[PROPERTY_NAME]).toBe('${var_1.orders[1].tags[1]}');
        });

        test('should offer the sample items of the array the index belongs to', async ({authenticatedPage: page}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'tags',
                configurationPanel,
                expectedReference: 'var_1.orders[0].tags[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const tagIndexPopover = await openArrayIndexPopover({
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).nth(1),
                page,
            });

            await expect(tagIndexPopover.getByRole('option')).toHaveText(['0"red"', '1"green"']);
        });
    });

    test.describe('Arrays longer than the listed rows', () => {
        test('should cap the listed rows and explain how to reach the rest', async ({authenticatedPage: page}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'many',
                configurationPanel,
                expectedReference: 'var_1.many[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const arrayIndexPopover = await openArrayIndexPopover({
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                page,
            });

            await expect(arrayIndexPopover.getByText(`${MANY_ITEM_COUNT} items`)).toBeVisible();

            await expect(arrayIndexPopover.getByRole('option')).toHaveCount(MAX_LISTED_SAMPLE_ROWS);

            await expect(
                arrayIndexPopover.getByText(
                    `Showing the first ${MAX_LISTED_SAMPLE_ROWS} items. Type an index to go further.`
                )
            ).toBeVisible();
        });

        test('should apply an index typed beyond the listed rows', async ({authenticatedPage: page, workflow}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'many',
                configurationPanel,
                expectedReference: 'var_1.many[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            await applyTypedArrayIndex({
                arrayIndex: MANY_ITEM_COUNT - 1,
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                expectedReference: `var_1.many[${MANY_ITEM_COUNT - 1}]`,
                page,
            });

            await expect(getMentionsInput(configurationPanel, PROPERTY_LABEL)).toHaveText(
                `var_1.many[${MANY_ITEM_COUNT - 1}]`
            );

            const workflowDefinition = await getWorkflowDefinition(page, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.[PROPERTY_NAME]).toBe(`\${var_1.many[${MANY_ITEM_COUNT - 1}]}`);
        });

        test('should keep Apply disabled while the typed index is empty or negative', async ({
            authenticatedPage: page,
        }) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'many',
                configurationPanel,
                expectedReference: 'var_1.many[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            const arrayIndexPopover = await openArrayIndexPopover({
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                page,
            });

            const arrayIndexInput = arrayIndexPopover.getByRole('spinbutton');
            const applyButton = arrayIndexPopover.getByRole('button', {name: 'Apply'});

            await arrayIndexInput.fill('');

            await expect(applyButton).toBeDisabled();

            await arrayIndexInput.fill('-1');

            await expect(applyButton).toBeDisabled();

            await arrayIndexInput.fill('3');

            await expect(applyButton).toBeEnabled();
        });
    });

    test.describe('Persistence', () => {
        test('should keep the picked index after a reload', async ({authenticatedPage: page}) => {
            await insertArrayItemDataPill({
                arrayPropertyName: 'fruits',
                configurationPanel,
                expectedReference: 'var_1.fruits[0]',
                page,
                propertyLabel: PROPERTY_LABEL,
            });

            await pickArrayIndex({
                arrayIndex: 2,
                arrayIndexTrigger: getArrayIndexTriggers(configurationPanel, PROPERTY_LABEL).first(),
                expectedReference: 'var_1.fruits[2]',
                page,
            });

            await page.reload();

            const reloadedConfigurationPanel = await openPropertyTestingPanel(page, PROPERTY_LABEL);

            await expect(getMentionsInput(reloadedConfigurationPanel, PROPERTY_LABEL)).toHaveText('var_1.fruits[2]');

            const reloadedArrayIndexTrigger = reloadedConfigurationPanel.getByRole('button', {
                name: arrayIndexTriggerName(2),
            });

            await expect(reloadedArrayIndexTrigger).toBeVisible();
            await expect(reloadedArrayIndexTrigger).toBeEnabled();
        });
    });
});
