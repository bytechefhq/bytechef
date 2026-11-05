import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {WorkflowPage} from '../../pages/workflowPage';
import {clickAndExpectToBeVisible} from '../../utils/clickAndExpectToBeVisible';
import {TIMEOUTS} from '../../utils/constants';
import {type WorkflowDefinitionType, getProjectWorkflows} from '../../utils/deploymentUtils';
import {getFormulaSwitch, getProperty} from '../../utils/formulaModesUtils';
import getRandomString from '../../utils/getRandomString';
import {countTaskExecutions, waitForJobStatus} from '../../utils/jobUtils';
import {validateWorkflowById} from '../../utils/workflowValidationUtils';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const WEBHOOK_TRIGGER = {name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'};

function uniqueLabel(prefix: string): string {
    return `${prefix} ${getRandomString().slice(0, 8)}`;
}

function buildBranchWorkflow(label: string, expression?: string): WorkflowDefinitionType {
    const branchParameters: Record<string, unknown> = {
        cases: [
            {
                key: 'first',
                tasks: [{name: 'caseTask', parameters: {type: 'STRING', value: 'case output'}, type: 'var/v1/set'}],
            },
        ],
        default: [{name: 'defaultTask', parameters: {type: 'STRING', value: 'default output'}, type: 'var/v1/set'}],
    };

    if (expression !== undefined) {
        branchParameters.expression = expression;
    }

    return {
        description: label,
        label,
        tasks: [{label: 'Branch', name: 'branch_1', parameters: branchParameters, type: 'branch/v1'}],
        triggers: [WEBHOOK_TRIGGER],
    };
}

function buildRawConditionWorkflow(label: string, expression: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                label: 'Condition',
                name: 'condition_1',
                parameters: {
                    caseFalse: [{name: 'falseTask', parameters: {type: 'STRING', value: 'no'}, type: 'var/v1/set'}],
                    caseTrue: [{name: 'trueTask', parameters: {type: 'STRING', value: 'yes'}, type: 'var/v1/set'}],
                    expression,
                    rawExpression: true,
                },
                type: 'condition/v1',
            },
        ],
        triggers: [WEBHOOK_TRIGGER],
    };
}

function buildMapItemWorkflow(label: string, itemReference: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                label: 'Var',
                name: 'var_1',
                parameters: {type: 'ARRAY', value: [{city: 'Zagreb', count: 3}]},
                type: 'var/v1/set',
            },
            {
                label: 'Map',
                name: 'map_1',
                parameters: {
                    items: '${var_1}',
                    iteratee: [
                        {
                            label: 'Logger',
                            name: 'logger_1',
                            parameters: {text: itemReference},
                            type: 'logger/v1/info',
                        },
                    ],
                },
                type: 'map/v1',
            },
        ],
        triggers: [WEBHOOK_TRIGGER],
    };
}

async function importAndGetWorkflowIds(
    importWorkflows: (projectId: string, definitions: WorkflowDefinitionType[]) => Promise<void>,
    page: Page,
    projectId: string,
    definition: WorkflowDefinitionType
): Promise<{projectWorkflowId: number; workflowId: string}> {
    await importWorkflows(projectId, [definition]);

    const projectWorkflow = (await getProjectWorkflows(page, projectId)).find(
        (workflow) => workflow.label === definition.label
    );

    if (!projectWorkflow) {
        throw new Error(`Project ${projectId} has no workflow labelled ${definition.label}`);
    }

    return {projectWorkflowId: projectWorkflow.projectWorkflowId, workflowId: projectWorkflow.id};
}

test.describe('Task dispatchers', () => {
    test.describe('Branch', () => {
        test('runs the default tasks when the expression is missing', async ({authenticatedPage, deployer}) => {
            const deployedProject = await deployer.deploy([buildBranchWorkflow(uniqueLabel('Branch no expression'))]);

            const job = await deployer.triggerWebhookAndWaitForJob(deployedProject);

            await waitForJobStatus(authenticatedPage, job.id, 'COMPLETED');

            expect(await countTaskExecutions(authenticatedPage, job.id, 'defaultTask')).toBe(1);
            expect(await countTaskExecutions(authenticatedPage, job.id, 'caseTask')).toBe(0);
        });

        test('runs the matching case when the expression names it', async ({authenticatedPage, deployer}) => {
            const deployedProject = await deployer.deploy([
                buildBranchWorkflow(uniqueLabel('Branch matching case'), 'first'),
            ]);

            const job = await deployer.triggerWebhookAndWaitForJob(deployedProject);

            await waitForJobStatus(authenticatedPage, job.id, 'COMPLETED');

            expect(await countTaskExecutions(authenticatedPage, job.id, 'caseTask')).toBe(1);
            expect(await countTaskExecutions(authenticatedPage, job.id, 'defaultTask')).toBe(0);
        });
    });

    test.describe('Validation', () => {
        test('reports a required raw Condition expression holding only = as missing', async ({
            authenticatedPage,
            deployer,
            project,
        }) => {
            const emptyFormulaWorkflow = await importAndGetWorkflowIds(
                deployer.importWorkflows,
                authenticatedPage,
                project.id,
                buildRawConditionWorkflow(uniqueLabel('Condition empty formula'), '=')
            );

            const emptyFormulaResult = await validateWorkflowById(authenticatedPage, emptyFormulaWorkflow.workflowId);

            expect(
                emptyFormulaResult.nodeIssues.filter(
                    (nodeIssue) => nodeIssue.nodeName === 'condition_1' && nodeIssue.propertyPath === 'expression'
                )
            ).not.toHaveLength(0);

            const filledFormulaWorkflow = await importAndGetWorkflowIds(
                deployer.importWorkflows,
                authenticatedPage,
                project.id,
                buildRawConditionWorkflow(uniqueLabel('Condition filled formula'), '=1 + 1 == 2')
            );

            const filledFormulaResult = await validateWorkflowById(authenticatedPage, filledFormulaWorkflow.workflowId);

            expect(
                filledFormulaResult.nodeIssues.filter(
                    (nodeIssue) => nodeIssue.nodeName === 'condition_1' && nodeIssue.propertyPath === 'expression'
                )
            ).toHaveLength(0);
        });

        test('accepts a Map iteratee reference to a field of the current item', async ({
            authenticatedPage,
            deployer,
            project,
        }) => {
            const mapWorkflow = await importAndGetWorkflowIds(
                deployer.importWorkflows,
                authenticatedPage,
                project.id,
                buildMapItemWorkflow(uniqueLabel('Map item field'), '${map_1.item.city}')
            );

            const validationResult = await validateWorkflowById(authenticatedPage, mapWorkflow.workflowId);

            expect(validationResult.errors).toEqual([]);
            expect(validationResult.nodeIssues.filter((nodeIssue) => nodeIssue.nodeName === 'logger_1')).toEqual([]);
        });
    });

    test.describe('Expression field', () => {
        for (const {definition, nodeName} of [
            {
                definition: (label: string) => buildRawConditionWorkflow(label, '=1 + 1 == 2'),
                nodeName: 'condition_1',
            },
            {definition: (label: string) => buildBranchWorkflow(label, 'first'), nodeName: 'branch_1'},
        ]) {
            test(`shows a checked, disabled Formula switch on the ${nodeName} expression`, async ({
                authenticatedPage: page,
                deployer,
                project,
            }) => {
                const {projectWorkflowId} = await importAndGetWorkflowIds(
                    deployer.importWorkflows,
                    page,
                    project.id,
                    definition(uniqueLabel(`Expression ${nodeName}`))
                );

                await new WorkflowPage(page).goToWorkflowEditor(project.id, String(projectWorkflowId));

                const workflowNode = page.getByLabel(`${nodeName} node`, {exact: true});
                const configurationPanel = page.getByLabel(`${nodeName} component configuration panel`);

                await expect(workflowNode).toBeVisible({timeout: TIMEOUTS.EDITOR_CANVAS_READY});

                await clickAndExpectToBeVisible({target: configurationPanel, trigger: workflowNode});

                const propertiesTabButton = configurationPanel.getByRole('button', {exact: true, name: 'Properties'});
                const expressionProperty = getProperty(configurationPanel, 'expression');

                await expect(propertiesTabButton).toBeVisible({timeout: TIMEOUTS.NODE_DETAILS_PANEL_READY});

                await clickAndExpectToBeVisible({target: expressionProperty, trigger: propertiesTabButton});

                const formulaSwitch = getFormulaSwitch(expressionProperty);

                await expect(formulaSwitch).toBeChecked();
                await expect(formulaSwitch).toBeDisabled();

                await formulaSwitch.click({force: true});

                await expect(formulaSwitch).toBeChecked();

                await formulaSwitch.locator('xpath=ancestor::span[1]').hover();

                await expect(page.getByRole('tooltip')).toContainText('This field always takes a formula');
            });
        }
    });
});
