import {type APIRequestContext, type Page, type Request, expect} from '@playwright/test';
import {readFileSync} from 'fs';

import {embeddedRolesTest as test} from '../../fixtures/embeddedRoles';
import {SAMPLE_WORKFLOW_PATH} from '../../utils/constants';
import {embeddedInternalApi, openAsEmbeddedRoleUser} from '../../utils/embeddedRbacUtils';
import getRandomString from '../../utils/getRandomString';
import {ENVIRONMENT_IDS, RBAC_BASE_URL, graphql, sendInternalApi} from '../../utils/rbacUtils';

const SAMPLE_WORKFLOW_NODE_NAME = 'var_1';

interface IntegrationWorkflowI {
    __version: number;
    definition: string;
    id: string;
    integrationWorkflowId: number;
}

interface EditorResourcesI {
    hubProjectId: string;
    hubWorkflowUuid: string;
    integrationId: number;
    integrationWorkflow: IntegrationWorkflowI;
}

function isWriteRequest(request: Request): boolean {
    const url = request.url();

    if (url.includes('/graphql')) {
        return (request.postData() || '').trimStart().startsWith('mutation');
    }

    return url.includes('/api/') && request.method() !== 'GET';
}

async function createEditorResources(adminApi: APIRequestContext): Promise<EditorResourcesI> {
    const suffix = getRandomString().slice(0, 8);

    const integrationId = await embeddedInternalApi<number>(adminApi, 'POST', '/integrations', {
        componentName: 'httpClient',
        multipleInstances: false,
        name: `rbac_read_only_${suffix}`,
    });

    await embeddedInternalApi(adminApi, 'POST', `/integrations/${integrationId}/workflows`, {
        definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'),
    });

    const [integrationWorkflow] = await embeddedInternalApi<IntegrationWorkflowI[]>(
        adminApi,
        'GET',
        `/integrations/${integrationId}/workflows`
    );

    const {createAutomationWorkflowProject: hubProjectId} = await graphql<{createAutomationWorkflowProject: string}>(
        adminApi,
        'mutation CreateAutomationWorkflowProject($name: String!) { createAutomationWorkflowProject(name: $name) }',
        {name: `rbac_read_only_${suffix}`}
    );

    const {createAutomationWorkflowProjectWorkflow: hubWorkflowUuid} = await graphql<{
        createAutomationWorkflowProjectWorkflow: string;
    }>(
        adminApi,
        `
            mutation CreateAutomationWorkflowProjectWorkflow($projectId: ID!, $definition: String) {
                createAutomationWorkflowProjectWorkflow(projectId: $projectId, definition: $definition)
            }
        `,
        {definition: readFileSync(SAMPLE_WORKFLOW_PATH, 'utf-8'), projectId: hubProjectId}
    );

    return {hubProjectId, hubWorkflowUuid, integrationId, integrationWorkflow};
}

async function deleteEditorResources(adminApi: APIRequestContext, resources: EditorResourcesI): Promise<void> {
    await graphql(
        adminApi,
        'mutation DeleteAutomationWorkflowProject($id: ID!) { deleteAutomationWorkflowProject(id: $id) }',
        {
            id: resources.hubProjectId,
        }
    ).catch((error: unknown) => console.warn(`readOnlyEditor teardown: Hub project: ${error}`));

    await embeddedInternalApi(adminApi, 'DELETE', `/integrations/${resources.integrationId}`).catch((error: unknown) =>
        console.warn(`readOnlyEditor teardown: integration: ${error}`)
    );
}

async function openIntegrationEditor(page: Page, resources: EditorResourcesI): Promise<void> {
    await page.goto(
        `/embedded/integrations/${resources.integrationId}/integration-workflows/${resources.integrationWorkflow.integrationWorkflowId}`
    );

    await expect(page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true})).toBeVisible({timeout: 30_000});
}

async function openHubEditor(page: Page, resources: EditorResourcesI): Promise<void> {
    await page.goto(`/embedded/automation-workflows/${resources.hubWorkflowUuid}/editor`);

    await expect(page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true})).toBeVisible({timeout: 30_000});
}

async function expectReadOnlyEditor(page: Page): Promise<void> {
    await expect(page.getByRole('status', {name: 'View only'})).toBeVisible();

    await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toHaveCount(0);
    await expect(page.getByTitle('Click to add a node')).toHaveCount(0);

    await page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true}).hover();

    await page.getByRole('button', {name: `${SAMPLE_WORKFLOW_NODE_NAME} node actions`}).click();

    await expect(page.getByRole('menuitem', {name: 'Info'})).toBeVisible();
    await expect(page.getByRole('menuitem', {name: /Delete|Cut|Copy|Rename/})).toHaveCount(0);

    await page.keyboard.press('Escape');
}

async function expectEditableEditor(page: Page): Promise<void> {
    await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toBeVisible();
    await expect(page.getByRole('status', {name: 'View only'})).toHaveCount(0);
}

test.describe('Embedded workflow editors', () => {
    test.describe.configure({mode: 'serial'});

    let resources: EditorResourcesI;

    test.beforeAll(async ({adminApi}) => {
        resources = await createEditorResources(adminApi);
    });

    test.afterAll(async ({adminApi}) => {
        if (resources) {
            await deleteEditorResources(adminApi, resources);
        }
    });

    test('a VIEWER opens the Integration editor read-only and sends no writes', async ({
        browser,
        embeddedRoleUsers,
    }) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.VIEWER,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        const writeRequests: string[] = [];

        page.on('request', (request) => {
            if (isWriteRequest(request)) {
                writeRequests.push(`${request.method()} ${request.url()}`);
            }
        });

        try {
            await openIntegrationEditor(page, resources);

            await expectReadOnlyEditor(page);

            await page.waitForLoadState('networkidle');

            expect(writeRequests).toEqual([]);

            const saveResponse = await sendInternalApi(
                page.request,
                'PUT',
                `/api/embedded/internal/workflows/${resources.integrationWorkflow.id}`,
                {
                    __version: resources.integrationWorkflow.__version,
                    definition: resources.integrationWorkflow.definition,
                }
            );

            expect(saveResponse.status()).toBe(403);
        } finally {
            await context.close();
        }
    });

    test('an EDITOR opens the Integration editor with its authoring controls', async ({browser, embeddedRoleUsers}) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.EDITOR,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openIntegrationEditor(page, resources);

            await expectEditableEditor(page);
        } finally {
            await context.close();
        }
    });

    test('a VIEWER opens the Hub editor read-only and sends no writes', async ({browser, embeddedRoleUsers}) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.VIEWER,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        const writeRequests: string[] = [];

        page.on('request', (request) => {
            if (isWriteRequest(request)) {
                writeRequests.push(`${request.method()} ${request.url()}`);
            }
        });

        try {
            await openHubEditor(page, resources);

            await expectReadOnlyEditor(page);

            await page.waitForTimeout(3000);

            expect(writeRequests).toEqual([]);
        } finally {
            await context.close();
        }
    });

    test('an EDITOR opens the Hub editor with its authoring controls', async ({browser, embeddedRoleUsers}) => {
        const {context, page} = await openAsEmbeddedRoleUser(
            browser,
            RBAC_BASE_URL,
            embeddedRoleUsers.EDITOR,
            ENVIRONMENT_IDS.DEVELOPMENT
        );

        try {
            await openHubEditor(page, resources);

            await expectEditableEditor(page);
        } finally {
            await context.close();
        }
    });
});
