import {type Locator, type Page, expect, mergeTests} from '@playwright/test';

import {importPropertyFormulaArrayItemWorkflowTest, loginTest, projectTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import propertyFormulaArrayItemWorkflow from '../../propertyFormulaArrayItemWorkflow.json';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {TIMEOUTS} from '../../utils/constants';
import {getFormulaSwitch} from '../../utils/formulaModesUtils';
import {nodeParametersSavePromise} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importPropertyFormulaArrayItemWorkflowTest);

const TRIGGER_NAME = 'trigger_1';

const FORM_INPUTS = propertyFormulaArrayItemWorkflow.triggers[0].parameters.inputs;

function getArrayItem(configurationPanel: Locator, index: number): Locator {
    return configurationPanel.getByLabel(`Array property item at index ${index}`, {exact: true});
}

async function getStoredFormInputs(page: Page, workflowId: string): Promise<unknown> {
    const response = await page.request.get(`/api/automation/internal/workflows/by-project-workflow-id/${workflowId}`);

    const workflow = await response.json();

    const definition = JSON.parse(workflow.definition);

    return definition.triggers[0].parameters?.inputs;
}

async function openFormInputs(page: Page): Promise<Locator> {
    const triggerNode = page.getByLabel(`${TRIGGER_NAME} node`, {exact: true});

    const configurationPanel = page.getByLabel(`${TRIGGER_NAME} component configuration panel`);

    await expect(triggerNode).toBeVisible({timeout: TIMEOUTS.EDITOR_CANVAS_READY});

    await clickAndExpectToBeVisible({target: configurationPanel, trigger: triggerNode});

    const propertiesTabButton = configurationPanel.getByRole('button', {exact: true, name: 'Properties'});

    await expect(propertiesTabButton).toBeVisible({timeout: TIMEOUTS.NODE_DETAILS_PANEL_READY});

    await clickAndExpectToBeVisible({target: getArrayItem(configurationPanel, 3), trigger: propertiesTabButton});

    return configurationPanel;
}

async function expectSiblingItemsIntact(configurationPanel: Locator): Promise<void> {
    for (const [index, formInput] of FORM_INPUTS.slice(0, 3).entries()) {
        const arrayItem = getArrayItem(configurationPanel, index);

        await expect(arrayItem.getByText(formInput.fieldLabel, {exact: true})).toBeVisible();
        await expect(getFormulaSwitch(arrayItem).first()).not.toBeChecked();
    }
}

test.describe('Formula switch on an object array item', () => {
    test('should turn only the fourth form input into a formula and keep it after a reload', async ({
        authenticatedPage: page,
        project,
        workflow,
    }) => {
        const workflowPage = new WorkflowPage(page);

        await workflowPage.goToWorkflowEditor(project.id, workflow.workflowId);

        let configurationPanel = await openFormInputs(page);

        await test.step('Turn Formula on for the fourth item and confirm the replacement', async () => {
            const fourthItem = getArrayItem(configurationPanel, 3);

            const saveResponsePromise = nodeParametersSavePromise({
                nodeName: TRIGGER_NAME,
                page,
                valueInBody: '"path":"inputs[3]"',
            });

            await getFormulaSwitch(fourthItem).first().click();

            await page.getByRole('button', {exact: true, name: 'Replace'}).click();

            await saveResponsePromise;

            await expect(getFormulaSwitch(fourthItem).first()).toBeChecked();
        });

        await test.step('Keep the first three items and their order', async () => {
            await expectSiblingItemsIntact(configurationPanel);

            await expect
                .poll(() => getStoredFormInputs(page, workflow.workflowId))
                .toEqual([...FORM_INPUTS.slice(0, 3), '=']);
        });

        await test.step('Keep the formula item and its siblings after a reload', async () => {
            await page.reload();

            configurationPanel = await openFormInputs(page);

            await expect(getFormulaSwitch(getArrayItem(configurationPanel, 3)).first()).toBeChecked();

            await expectSiblingItemsIntact(configurationPanel);

            await page.waitForTimeout(WorkflowPage.LONG_DEBOUNCE_MS * 2);

            expect(await getStoredFormInputs(page, workflow.workflowId)).toEqual([...FORM_INPUTS.slice(0, 3), '=']);
        });
    });
});
