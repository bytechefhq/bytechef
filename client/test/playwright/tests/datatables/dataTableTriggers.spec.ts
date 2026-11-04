import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID, graphqlData} from '../../utils/apiUtils';
import {type WorkflowDefinitionType} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {type JobI, getProjectJobs} from '../../utils/jobUtils';

const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const STAGING_ENVIRONMENT_ID = 1;

const CREATED_LABEL = 'Data table record created';

const UPDATED_LABEL = 'Data table record updated';

const DELETED_LABEL = 'Data table record deleted';

const ACTION_LABEL = 'Data table create records action';

interface DataTableI {
    baseName: string;
    id: string;
}

interface TriggerPayloadI {
    id: number | string;
    name?: string | null;
}

function triggerWorkflow(label: string, triggerName: string, baseName: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [{name: 'var_1', parameters: {type: 'STRING', value: 'received'}, type: 'var/v1/set'}],
        triggers: [{name: 'trigger_1', parameters: {table: baseName}, type: `dataTable/v1/${triggerName}`}],
    };
}

async function createDataTable(
    page: Page,
    workspaceId: number,
    environmentId: number,
    baseName: string
): Promise<DataTableI> {
    await graphqlData(
        page,
        'mutation createDataTable($input: CreateDataTableInput!) { createDataTable(input: $input) }',
        {
            input: {
                baseName,
                columns: [{name: 'name', type: 'STRING'}],
                environmentId: String(environmentId),
                workspaceId: String(workspaceId),
            },
        }
    );

    const {dataTables} = await graphqlData<{dataTables: DataTableI[]}>(
        page,
        'query dataTables($environmentId: ID!, $workspaceId: ID!) { dataTables(environmentId: $environmentId, workspaceId: $workspaceId) { baseName id } }',
        {environmentId: String(environmentId), workspaceId: String(workspaceId)}
    );

    const dataTable = dataTables.find((table) => table.baseName === baseName);

    if (!dataTable) {
        throw new Error(`Data table ${baseName} was not created`);
    }

    return dataTable;
}

async function dropDataTable(page: Page, dataTable: DataTableI, environmentId: number): Promise<void> {
    await graphqlData(page, 'mutation dropDataTable($input: RemoveTableInput!) { dropDataTable(input: $input) }', {
        input: {environmentId: String(environmentId), tableId: dataTable.id},
    }).catch(() => undefined);
}

async function insertRow(
    page: Page,
    dataTable: DataTableI,
    environmentId: number,
    values: Record<string, unknown>
): Promise<string> {
    const {insertDataTableRow} = await graphqlData<{insertDataTableRow: {id: string}}>(
        page,
        'mutation insertDataTableRow($input: InsertRowInput!) { insertDataTableRow(input: $input) { id } }',
        {input: {environmentId: String(environmentId), tableId: dataTable.id, values}}
    );

    return insertDataTableRow.id;
}

async function listWebhookTypes(page: Page, dataTable: DataTableI, environmentId: number): Promise<string[]> {
    const {dataTableWebhooks} = await graphqlData<{dataTableWebhooks: Array<{type: string}>}>(
        page,
        'query dataTableWebhooks($environmentId: ID!, $tableId: ID!) { dataTableWebhooks(environmentId: $environmentId, tableId: $tableId) { type } }',
        {environmentId: String(environmentId), tableId: dataTable.id}
    );

    return dataTableWebhooks.map((dataTableWebhook) => dataTableWebhook.type).sort();
}

function triggerPayload(job: JobI): TriggerPayloadI {
    return (job as JobI & {inputs: {trigger_1: TriggerPayloadI}}).inputs.trigger_1;
}

test.describe('Data Table triggers', () => {
    test('row changes made in the table editor and by a workflow action start the deployed trigger workflows', async ({
        authenticatedPage,
        deployer,
    }) => {
        const page = authenticatedPage;

        const baseName = `e2e_triggers_${getRandomString().slice(0, 8).toLowerCase()}`;

        const dataTable = await createDataTable(page, deployer.workspaceId, DEVELOPMENT_ENVIRONMENT_ID, baseName);
        const stagingDataTable = await createDataTable(
            page,
            deployer.workspaceId,
            STAGING_ENVIRONMENT_ID,
            baseName
        ).catch(() => undefined);

        const jobsOf = async (label: string): Promise<JobI[]> => {
            const projectJobs = await getProjectJobs(page, deployer.workspaceId, deployedProject.projectId);

            return projectJobs.filter((job) => job.label === label);
        };

        const waitForJobs = async (label: string, count: number): Promise<JobI[]> => {
            let labelJobs: JobI[] = [];

            await expect(async () => {
                labelJobs = await jobsOf(label);

                expect(labelJobs).toHaveLength(count);
            }).toPass({intervals: [500, 1000, 2000], timeout: 30000});

            return labelJobs;
        };

        const deployedProject = await deployer.deploy([
            triggerWorkflow(CREATED_LABEL, 'recordCreated', dataTable.baseName),
            triggerWorkflow(UPDATED_LABEL, 'recordUpdated', dataTable.baseName),
            triggerWorkflow(DELETED_LABEL, 'recordDeleted', dataTable.baseName),
            {
                description: ACTION_LABEL,
                label: ACTION_LABEL,
                tasks: [
                    {
                        name: 'dataTable_1',
                        parameters: {
                            records: {values: [{name: 'created by an action'}]},
                            table: dataTable.baseName,
                        },
                        type: 'dataTable/v1/createRecords',
                    },
                ],
                triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
            },
        ]);

        try {
            await test.step('Enabling the deployment registers one trigger per event type', async () => {
                expect(await listWebhookTypes(page, dataTable, DEVELOPMENT_ENVIRONMENT_ID)).toEqual([
                    'RECORD_CREATED',
                    'RECORD_DELETED',
                    'RECORD_UPDATED',
                ]);
            });

            await page.goto(`/automation/datatables/${dataTable.id}`);

            const grid = page.getByRole('grid');

            await expect(grid).toBeVisible();

            await test.step('Adding a row in the editor starts the Record Created workflow', async () => {
                await page.getByTitle('Add row').click();

                const [createdJob] = await waitForJobs(CREATED_LABEL, 1);

                expect(triggerPayload(createdJob).id).toBeTruthy();
            });

            await test.step('Editing a cell starts the Record Updated workflow with the new value', async () => {
                const nameCell = grid.getByRole('row').nth(1).getByRole('gridcell').nth(1);

                await nameCell.dblclick();

                await page.keyboard.type('edited in the editor');

                await page.keyboard.press('Enter');

                const [updatedJob] = await waitForJobs(UPDATED_LABEL, 1);

                expect(triggerPayload(updatedJob).name).toBe('edited in the editor');
                expect(String(triggerPayload(updatedJob).id)).toBe(
                    String(triggerPayload((await jobsOf(CREATED_LABEL))[0]).id)
                );
            });

            await test.step('Deleting the row starts the Record Deleted workflow', async () => {
                await page.getByTitle('Row 1', {exact: true}).hover();

                await page.getByLabel('Select row 1').click();

                await page.getByRole('button', {name: 'Delete (1)'}).click();

                await page.getByRole('alertdialog').getByRole('button', {name: 'Delete'}).click();

                const [deletedJob] = await waitForJobs(DELETED_LABEL, 1);

                expect(triggerPayload(deletedJob).name).toBe('edited in the editor');
            });

            await test.step('A Create Record(s) action in a workflow starts the Record Created workflow', async () => {
                await deployer.triggerWebhookAndWaitForJob(deployedProject, {}, ACTION_LABEL);

                const createdJobs = await waitForJobs(CREATED_LABEL, 2);

                expect(createdJobs.map((job) => triggerPayload(job).name)).toContain('created by an action');
            });

            await test.step('A row inserted in another environment does not start the workflows', async () => {
                test.skip(!stagingDataTable, 'The staging environment is not available on this server');

                expect(await listWebhookTypes(page, stagingDataTable!, STAGING_ENVIRONMENT_ID)).toEqual([]);

                await insertRow(page, stagingDataTable!, STAGING_ENVIRONMENT_ID, {name: 'staging row'});

                await insertRow(page, dataTable, DEVELOPMENT_ENVIRONMENT_ID, {name: 'development row'});

                const createdJobs = await waitForJobs(CREATED_LABEL, 3);

                expect(createdJobs.map((job) => triggerPayload(job).name)).not.toContain('staging row');

                await page.waitForTimeout(3000);

                expect(await jobsOf(CREATED_LABEL)).toHaveLength(3);
            });
        } finally {
            await dropDataTable(page, dataTable, DEVELOPMENT_ENVIRONMENT_ID);

            if (stagingDataTable) {
                await dropDataTable(page, stagingDataTable, STAGING_ENVIRONMENT_ID);
            }
        }
    });
});
