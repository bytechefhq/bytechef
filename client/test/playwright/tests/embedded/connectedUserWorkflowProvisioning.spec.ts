import {type Page, expect} from '@playwright/test';

import {loginTest} from '../../fixtures';
import getRandomString from '../../utils/getRandomString';
import {BackendSession} from '../../utils/mcpAuthentication/backendSession';
import {ENVIRONMENT_IDS, MCP_TEST_CONFIGURATION} from '../../utils/mcpAuthentication/mcpTestConfiguration';

const CREATE_AUTOMATION_PROJECT = `mutation createAutomationProject($name: String!) {
    createAutomationProject(name: $name)
}`;

const CREATE_AUTOMATION_PROJECT_WORKFLOW = `mutation createAutomationProjectWorkflow($projectId: ID!, $definition: String) {
    createAutomationProjectWorkflow(projectId: $projectId, definition: $definition)
}`;

const PUBLISH_AUTOMATION_PROJECT = `mutation publishAutomationProject($id: ID!) {
    publishAutomationProject(id: $id)
}`;

const CREATE_AUTOMATION_PROJECT_CONFIGURATION = `mutation createAutomationProjectConfiguration(
    $projectId: ID!, $environmentId: ID!, $name: String!
) {
    createAutomationProjectConfiguration(
        projectId: $projectId
        environmentId: $environmentId
        name: $name
        projectVersion: 1
        enabled: true
        automationHubVisible: true
    )
}`;

const CONNECTED_USER_PROJECTS = `query connectedUserProjects($connectedUserId: ID, $environmentId: ID) {
    connectedUserProjects(connectedUserId: $connectedUserId, environmentId: $environmentId) {
        connectedUserProjectWorkflows {
            enabled
            workflow {
                label
            }
        }
    }
}`;

const AIRTABLE_WORKFLOW_DEFINITION = {
    description: '',
    inputs: [{label: 'Record ID', name: 'recordId', required: true, type: 'string'}],
    label: 'Read Airtable record',
    tasks: [
        {
            label: 'Get Record',
            name: 'airtable_1',
            parameters: {baseId: 'appE2E', recordId: '${recordId}', tableId: 'tblE2E'},
            type: 'airtable/v1/getRecord',
        },
    ],
    triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
};

const GREETING_WORKFLOW_DEFINITION = {
    description: '',
    inputs: [],
    label: 'Set greeting',
    tasks: [{label: 'Var', name: 'var_1', parameters: {type: 'STRING', value: 'hello'}, type: 'var/v1/set'}],
    triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
};

interface ProvisioningFixtureI {
    configurationName: string;
    connectedUserExternalId: string;
    connectedUserId: number;
    projectName: string;
}

async function createProvisioningFixture(adminSession: BackendSession): Promise<ProvisioningFixtureI> {
    const namePrefix = `E2E ${getRandomString().slice(0, 8)}`;

    const projectName = `${namePrefix} Automations`;
    const configurationName = `${namePrefix} Configuration`;
    const connectedUserExternalId = `${namePrefix.replace(' ', '-').toLowerCase()}-user`;

    const {createAutomationProject: projectId} = await adminSession.graphQlData<{createAutomationProject: string}>(
        CREATE_AUTOMATION_PROJECT,
        {name: projectName}
    );

    for (const workflowDefinition of [AIRTABLE_WORKFLOW_DEFINITION, GREETING_WORKFLOW_DEFINITION]) {
        await adminSession.graphQlData(CREATE_AUTOMATION_PROJECT_WORKFLOW, {
            definition: JSON.stringify(workflowDefinition),
            projectId,
        });
    }

    await adminSession.graphQlData(PUBLISH_AUTOMATION_PROJECT, {id: projectId});

    await adminSession.graphQlData(CREATE_AUTOMATION_PROJECT_CONFIGURATION, {
        environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
        name: configurationName,
        projectId,
    });

    const connectedUserResponse = await adminSession.rest('POST', '/api/embedded/internal/connected-users', {
        environmentId: ENVIRONMENT_IDS.DEVELOPMENT,
        externalId: connectedUserExternalId,
    });

    expect(connectedUserResponse.ok()).toBe(true);

    const {id: connectedUserId} = (await connectedUserResponse.json()) as {id: number};

    return {configurationName, connectedUserExternalId, connectedUserId, projectName};
}

async function openConnectedUserAutomations(page: Page, connectedUserExternalId: string): Promise<void> {
    await page.goto('/embedded/connected-users');

    await page.getByRole('textbox', {name: 'Search'}).fill(connectedUserExternalId);

    await page.getByRole('cell', {exact: true, name: connectedUserExternalId}).click();

    await page.getByRole('tab', {name: 'Automations'}).click();
}

const test = loginTest();

test.describe('Connected user workflow provisioning', () => {
    let adminSession: BackendSession;

    test.beforeAll(async () => {
        adminSession = await BackendSession.signIn(MCP_TEST_CONFIGURATION.admin);
    });

    test.afterAll(async () => {
        await adminSession?.dispose();
    });

    test('provisions workflows with a new connection and their inputs, then groups them under their configuration', async ({
        authenticatedPage: page,
    }) => {
        const {configurationName, connectedUserExternalId, connectedUserId, projectName} =
            await createProvisioningFixture(adminSession);

        await openConnectedUserAutomations(page, connectedUserExternalId);

        await page.getByRole('button', {name: 'Provision Workflow'}).click();

        const provisionDialog = page.getByRole('dialog', {name: 'New Workflow'});

        await provisionDialog.getByRole('combobox', {name: 'Project'}).selectOption({label: `${projectName} V1`});

        await expect(provisionDialog.getByRole('combobox', {name: 'Configuration'})).toContainText(configurationName);

        await provisionDialog.getByRole('checkbox', {name: 'Read Airtable record'}).click();

        const airtableWorkflow = provisionDialog.getByRole('group', {name: 'Read Airtable record'});

        await expect(airtableWorkflow.getByText('No Airtable connection.', {exact: false})).toBeVisible();
        await expect(provisionDialog.getByRole('button', {name: 'Save'})).toBeDisabled();

        await airtableWorkflow.getByRole('button', {name: 'New Airtable connection'}).click();

        const connectionDialog = page.getByRole('dialog', {name: 'Create Connection'});

        await connectionDialog.getByRole('textbox', {name: 'Token'}).fill('e2e-token');

        await connectionDialog.getByRole('button', {name: 'Save'}).click();

        await expect(connectionDialog).toBeHidden();

        await expect(airtableWorkflow.getByRole('combobox', {name: 'Airtable connection'})).toContainText(
            'Provisioned'
        );
        await expect(provisionDialog.getByRole('button', {name: 'Save'})).toBeDisabled();

        await airtableWorkflow.getByRole('textbox', {name: 'Record ID'}).fill('rec-e2e');

        await provisionDialog.getByRole('checkbox', {name: 'Set greeting'}).click();

        await provisionDialog.getByRole('button', {name: 'Save'}).click();

        await expect(provisionDialog).toBeHidden();

        const projectGroup = page.getByRole('region', {name: projectName});

        await expect(projectGroup.getByText(configurationName, {exact: true})).toBeVisible();
        await expect(projectGroup.getByText('2 workflows')).toBeVisible();
        await expect(projectGroup.getByText('No configuration')).toHaveCount(0);

        const {connectedUserProjects} = await adminSession.graphQlData<{
            connectedUserProjects: Array<{
                connectedUserProjectWorkflows: Array<{enabled: boolean; workflow: {label: string}}>;
            }>;
        }>(CONNECTED_USER_PROJECTS, {
            connectedUserId: String(connectedUserId),
            environmentId: String(ENVIRONMENT_IDS.DEVELOPMENT),
        });

        const provisionedWorkflows = connectedUserProjects.flatMap(
            (connectedUserProject) => connectedUserProject.connectedUserProjectWorkflows
        );

        expect(provisionedWorkflows.map((provisionedWorkflow) => provisionedWorkflow.workflow.label).sort()).toEqual([
            'Read Airtable record',
            'Set greeting',
        ]);
        expect(
            provisionedWorkflows.find((provisionedWorkflow) => provisionedWorkflow.workflow.label === 'Set greeting')
                ?.enabled
        ).toBe(true);
    });
});
