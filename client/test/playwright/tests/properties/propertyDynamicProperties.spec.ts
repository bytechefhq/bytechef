import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {
    DYNAMIC_PROPERTIES_LOOKUP_LABEL,
    DYNAMIC_PROPERTIES_ROUTE,
    DYNAMIC_SUB_PROPERTY_ONE_LABEL,
    DYNAMIC_SUB_PROPERTY_ONE_NAME,
    DYNAMIC_SUB_PROPERTY_TWO_LABEL,
    dynamicPropertiesRequestPromise,
    getDynamicPropertyContainer,
    setDynamicPropertiesLookupValue,
} from '../../utils/dynamicPropertiesUtils';
import {type TestWorkflowI} from '../../utils/projectUtils';
import {
    fillInputAndWaitForSave,
    openPropertyTestingPanel,
    openPropertyTestingPanelAndPropertiesTab,
} from '../../utils/propertyValidationUtils';
import {getTaskParameters, getWorkflowDefinition} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importWorkflowTest);

const PROPERTY_TESTING_TASK_NAME = 'propertyTesting_1';

const LOOKUP_VALUE = 'lookup value';

test.describe('PropertyDynamicProperties - Dynamic properties type (PropertyDynamicProperties.tsx)', () => {
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
            DYNAMIC_PROPERTIES_LOOKUP_LABEL
        );
    });

    test.describe('Disabled query while the lookup dependency is unset', () => {
        test('should not render dynamic sub-properties or a skeleton before the lookup value is set', async () => {
            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toHaveCount(0);

            await expect(getDynamicPropertyContainer(configurationPanel).getByRole('textbox')).toHaveCount(0);

            await expect(configurationPanel.locator('[data-slot="skeleton"]')).toHaveCount(0);
        });

        test('should not issue a dynamic-properties request while the lookup value is empty', async () => {
            let dynamicPropertiesRequested = false;

            await authenticatedPage.route(DYNAMIC_PROPERTIES_ROUTE, async (route) => {
                dynamicPropertiesRequested = true;

                await route.continue();
            });

            await authenticatedPage.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS);

            expect(dynamicPropertiesRequested).toBe(false);
        });
    });

    test.describe('Rendering from the dynamic-properties response', () => {
        test('should render both dynamic sub-properties once the lookup value is set', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toBeVisible();

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_TWO_LABEL, {exact: true})).toBeVisible();
        });

        test('should render each dynamic sub-property with its label and a textbox control', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            const firstSubProperty = configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true});

            await expect(firstSubProperty.getByText('Dynamic Property 1')).toBeVisible();

            await expect(firstSubProperty.getByRole('textbox')).toBeVisible();
        });
    });

    test.describe('Loading state', () => {
        test('should render the dynamic-properties skeleton while the request is pending', async () => {
            let releaseDynamicProperties: () => void = () => {};

            const dynamicPropertiesGate = new Promise<void>((resolve) => {
                releaseDynamicProperties = resolve;
            });

            await authenticatedPage.route(DYNAMIC_PROPERTIES_ROUTE, async (route) => {
                await dynamicPropertiesGate;

                await route.continue();
            });

            await setDynamicPropertiesLookupValue({
                configurationPanel,
                page: authenticatedPage,
                value: LOOKUP_VALUE,
                waitForRequest: false,
            });

            const skeletons = configurationPanel.locator('[data-slot="skeleton"]');

            await expect(skeletons.first()).toBeVisible();

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toHaveCount(0);

            releaseDynamicProperties();

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toBeVisible();

            await expect(skeletons).toHaveCount(0);
        });

        test('should re-request dynamic properties when the lookup dependency changes', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toBeVisible();

            const secondRequestPromise = dynamicPropertiesRequestPromise(authenticatedPage);

            await setDynamicPropertiesLookupValue({
                configurationPanel,
                page: authenticatedPage,
                value: 'changed lookup value',
                waitForRequest: false,
            });

            await secondRequestPromise;

            await expect(configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})).toBeVisible();
        });
    });

    test.describe('Editing and persistence', () => {
        const subPropertyValue = 'dynamic sub value';

        test('should persist a dynamic sub-property value to the workflow definition', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            const firstSubPropertyInput = configurationPanel
                .getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})
                .getByRole('textbox');

            await fillInputAndWaitForSave({
                input: firstSubPropertyInput,
                page: authenticatedPage,
                value: subPropertyValue,
            });

            const workflowDefinition = await getWorkflowDefinition(authenticatedPage, workflow.workflowId);

            const taskParameters = getTaskParameters({
                taskName: PROPERTY_TESTING_TASK_NAME,
                workflowDefinition,
            });

            expect(taskParameters?.dynamicPropertiesLookup).toBe(LOOKUP_VALUE);

            const dynamicProperty = taskParameters?.dynamicProperty as Record<string, unknown> | undefined;

            expect(dynamicProperty?.[DYNAMIC_SUB_PROPERTY_ONE_NAME]).toBe(subPropertyValue);
        });

        test('should keep the dynamic sub-property value after a reload', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            const firstSubPropertyInput = configurationPanel
                .getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})
                .getByRole('textbox');

            await fillInputAndWaitForSave({
                input: firstSubPropertyInput,
                page: authenticatedPage,
                value: subPropertyValue,
            });

            await authenticatedPage.reload();

            await authenticatedPage.waitForLoadState('domcontentloaded');

            await authenticatedPage.waitForTimeout(2000);

            configurationPanel = await openPropertyTestingPanel(authenticatedPage, DYNAMIC_PROPERTIES_LOOKUP_LABEL);

            const reloadedSubProperty = configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true});

            await expect(reloadedSubProperty).toBeVisible();

            await expect(reloadedSubProperty.getByRole('textbox')).toHaveText(subPropertyValue);
        });

        test('should keep the dynamic sub-property value after switching nodes and returning', async () => {
            await setDynamicPropertiesLookupValue({configurationPanel, page: authenticatedPage, value: LOOKUP_VALUE});

            const firstSubPropertyInput = configurationPanel
                .getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true})
                .getByRole('textbox');

            await fillInputAndWaitForSave({
                input: firstSubPropertyInput,
                page: authenticatedPage,
                value: subPropertyValue,
            });

            await test.step('Open another node then return to propertyTesting_1', async () => {
                await clickAndExpectToBeVisible({
                    target: authenticatedPage.getByLabel('var_1 component configuration panel'),
                    trigger: authenticatedPage.getByLabel('var_1 node', {exact: true}),
                });

                configurationPanel = await openPropertyTestingPanel(authenticatedPage, DYNAMIC_PROPERTIES_LOOKUP_LABEL);
            });

            const reopenedSubProperty = configurationPanel.getByLabel(DYNAMIC_SUB_PROPERTY_ONE_LABEL, {exact: true});

            await expect(reopenedSubProperty).toBeVisible();

            await expect(reopenedSubProperty.getByRole('textbox')).toHaveText(subPropertyValue);
        });
    });
});
