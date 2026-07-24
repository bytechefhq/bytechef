import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {
    BOOLEAN_PROPERTY_LABEL,
    CONDITIONAL_PROPERTY_LABEL,
    getConditionalProperty,
    setBooleanPropertyValueAndWaitForSave,
} from '../../utils/displayConditionUtils';
import {type TestWorkflowI} from '../../utils/projectUtils';
import {
    fillInputAndWaitForSave,
    openPropertyTestingPanel,
    openPropertyTestingPanelAndPropertiesTab,
} from '../../utils/propertyValidationUtils';
import {getTaskParameters, getWorkflowDefinition} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

const DISPLAY_CONDITIONS_ROUTE =
    '**/api/platform/internal/workflows/*/workflow-nodes/propertyTesting_1/display-conditions*';

const PROPERTY_TESTING_TASK_NAME = 'propertyTesting_1';

test.describe('Property display conditions (displayCondition)', () => {
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
            BOOLEAN_PROPERTY_LABEL
        );
    });

    test.describe('Properties without a display condition', () => {
        test('should render properties that have no display condition regardless of the condition state', async () => {
            const unconditionalPropertyLabels = ['bool property', 'date property', 'dateTime property'];

            for (const propertyLabel of unconditionalPropertyLabels) {
                await expect(configurationPanel.getByLabel(propertyLabel, {exact: true})).toBeVisible();
            }
        });

        test('should not replace unconditional properties with a skeleton once the panel is loaded', async () => {
            await expect(configurationPanel.locator('[data-slot="skeleton"]')).toHaveCount(0);

            await expect(configurationPanel.getByLabel(BOOLEAN_PROPERTY_LABEL, {exact: true})).toBeVisible();
        });
    });

    test.describe('Hiding and showing based on the condition', () => {
        test('should not render the conditional property while the condition is false', async () => {
            await expect(getConditionalProperty(configurationPanel)).toHaveCount(0);
        });

        test('should render the conditional property once the condition becomes true', async () => {
            await setBooleanPropertyValueAndWaitForSave({configurationPanel, page: authenticatedPage, value: true});

            const conditionalProperty = getConditionalProperty(configurationPanel);

            await expect(conditionalProperty).toBeVisible();

            await expect(conditionalProperty.getByRole('textbox')).toBeVisible();
        });

        test('should hide the conditional property via its display condition after it was shown', async () => {
            await test.step('Reveal the conditional property by setting the condition to true', async () => {
                await setBooleanPropertyValueAndWaitForSave({configurationPanel, page: authenticatedPage, value: true});

                await expect(getConditionalProperty(configurationPanel)).toBeVisible();
            });

            await test.step('Hide it again by setting the condition to false', async () => {
                await setBooleanPropertyValueAndWaitForSave({
                    configurationPanel,
                    page: authenticatedPage,
                    value: false,
                });

                await expect(getConditionalProperty(configurationPanel)).toHaveCount(0);

                await expect(configurationPanel.locator('[data-slot="skeleton"]')).toHaveCount(0);
            });
        });
    });

    test.describe('Editing the conditional property', () => {
        const conditionalValue = 'conditional value';

        test('should persist a value entered into the conditional property', async () => {
            await setBooleanPropertyValueAndWaitForSave({configurationPanel, page: authenticatedPage, value: true});

            const conditionalInput = getConditionalProperty(configurationPanel).getByRole('textbox');

            await fillInputAndWaitForSave({input: conditionalInput, page: authenticatedPage, value: conditionalValue});

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.bool).toBe(true);
            expect(taskParameters?.displayCondition).toBe(conditionalValue);
        });

        test('should keep the conditional property visible with its value after a reload', async () => {
            await setBooleanPropertyValueAndWaitForSave({configurationPanel, page: authenticatedPage, value: true});

            const conditionalInput = getConditionalProperty(configurationPanel).getByRole('textbox');

            await fillInputAndWaitForSave({input: conditionalInput, page: authenticatedPage, value: conditionalValue});

            await authenticatedPage.reload();

            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);

            configurationPanel = await openPropertyTestingPanel(authenticatedPage, BOOLEAN_PROPERTY_LABEL);

            const reloadedConditionalProperty = getConditionalProperty(configurationPanel);

            await expect(reloadedConditionalProperty).toBeVisible();

            await expect(reloadedConditionalProperty.getByRole('textbox')).toHaveText(conditionalValue);
        });
    });
});

test.describe('Property display conditions - loading state', () => {
    test('should render a skeleton for the conditional property while display conditions are pending', async ({
        authenticatedPage,
        project,
        workflow,
    }) => {
        let releaseDisplayConditions: () => void = () => {};

        const displayConditionsGate = new Promise<void>((resolve) => {
            releaseDisplayConditions = resolve;
        });

        await authenticatedPage.route(DISPLAY_CONDITIONS_ROUTE, async (route) => {
            await displayConditionsGate;

            await route.continue();
        });

        const configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            authenticatedPage,
            project.id,
            workflow.workflowId,
            BOOLEAN_PROPERTY_LABEL
        );

        const skeletons = configurationPanel.locator('[data-slot="skeleton"]');

        await expect(skeletons.first()).toBeVisible();

        await expect(configurationPanel.getByLabel(CONDITIONAL_PROPERTY_LABEL, {exact: true})).toHaveCount(0);

        releaseDisplayConditions();

        await expect(skeletons).toHaveCount(0);

        await expect(configurationPanel.getByLabel(BOOLEAN_PROPERTY_LABEL, {exact: true})).toBeVisible();
    });
});
