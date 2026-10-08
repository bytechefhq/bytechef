import {type Locator, expect, mergeTests} from '@playwright/test';

import {importPropertyMentionsWorkflowTest, loginTest, projectTest} from '../../fixtures';
import propertyMentionsWorkflow from '../../propertyMentionsWorkflow.json';
import {getMentionsInput} from '../../utils/dataPillArrayIndexUtils';
import {
    openPropertyTestingPanelAndPropertiesTab,
    propertyTestingParametersSavePromise,
} from '../../utils/propertyValidationUtils';
import {getTaskParameters, getWorkflowDefinition} from '../../utils/workflowUtils';

export const test = mergeTests(loginTest(), projectTest, importPropertyMentionsWorkflowTest);

const PROPERTY_TESTING_TASK_NAME = 'propertyTesting_1';

const MENTION_SELECTOR = '.property-mention[data-id]';

const propertyTestingTask = propertyMentionsWorkflow.tasks.find((task) => task.name === PROPERTY_TESTING_TASK_NAME)!;

const storedParameters = propertyTestingTask.parameters;

/**
 * A stored text value is turned into editor html on load. Markup in the text has to survive as text, and every
 * ${...} data pill has to become a mention, including pills whose bracketed path holds an `&`.
 */
test.describe('Property mentions', () => {
    let configurationPanel: Locator;

    test.beforeEach(async ({authenticatedPage: page, project, workflow}) => {
        configurationPanel = await openPropertyTestingPanelAndPropertiesTab(
            page,
            project.id,
            workflow.workflowId,
            'stringMinLength property'
        );
    });

    test.describe('Loading a stored value', () => {
        test('should show angle brackets around a data pill as text and the pill as a mention', async () => {
            const mentionsInput = getMentionsInput(configurationPanel, 'stringMinLength property');

            await expect(mentionsInput).toContainText('Hey <!everyone>, look at <https://www.bytechef.io/|page>');
            await expect(mentionsInput).toContainText(':smile:');
            await expect(mentionsInput).not.toContainText('&lt;');
            await expect(mentionsInput).not.toContainText('${var_1.String}');

            const mentions = mentionsInput.locator(MENTION_SELECTOR);

            await expect(mentions).toHaveCount(1);
            await expect(mentions).toHaveAttribute('data-id', 'var_1.String');
        });

        test('should render a pill whose bracketed path holds an ampersand as a mention', async () => {
            const mentionsInput = getMentionsInput(configurationPanel, 'stringRegEx property');

            await expect(mentionsInput).toContainText('& more');
            await expect(mentionsInput).not.toContainText('&amp;');
            await expect(mentionsInput).not.toContainText("${var_1['Q&A']}");

            const mentions = mentionsInput.locator(MENTION_SELECTOR);

            await expect(mentions).toHaveCount(1);
            await expect(mentions).toHaveAttribute('data-id', "var_1['Q&A']");
        });
    });

    test.describe('Saving an edited value', () => {
        test('should save angle brackets and the data pill back unchanged when text is appended', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const mentionsInput = getMentionsInput(configurationPanel, 'stringMinLength property');

            await expect(mentionsInput.locator(MENTION_SELECTOR)).toHaveCount(1);

            await test.step('Append text at the end of the value', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, ':smile: ok');

                await mentionsInput.click();
                await mentionsInput.press('End');

                await page.keyboard.type(' ok');

                await saveResponsePromise;
            });

            await test.step('Verify the workflow definition holds the raw text and the pill', async () => {
                const workflowDefinition = await getWorkflowDefinition(page, workflow.workflowId);

                const parameters = getTaskParameters({taskName: PROPERTY_TESTING_TASK_NAME, workflowDefinition});

                expect(parameters?.stringMinLength).toBe(`${storedParameters.stringMinLength} ok`);
            });
        });

        test('should save a pill whose bracketed path holds an ampersand back unchanged', async ({
            authenticatedPage: page,
            workflow,
        }) => {
            const mentionsInput = getMentionsInput(configurationPanel, 'stringRegEx property');

            await expect(mentionsInput.locator(MENTION_SELECTOR)).toHaveCount(1);

            await test.step('Append text at the end of the value', async () => {
                const saveResponsePromise = propertyTestingParametersSavePromise(page, '& more ok');

                await mentionsInput.click();
                await mentionsInput.press('End');

                await page.keyboard.type(' ok');

                await saveResponsePromise;
            });

            await test.step('Verify the workflow definition holds the raw text and the pill', async () => {
                const workflowDefinition = await getWorkflowDefinition(page, workflow.workflowId);

                const parameters = getTaskParameters({taskName: PROPERTY_TESTING_TASK_NAME, workflowDefinition});

                expect(parameters?.stringRegEx).toBe(`${storedParameters.stringRegEx} ok`);
            });
        });
    });
});
