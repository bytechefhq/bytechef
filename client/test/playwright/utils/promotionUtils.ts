import {type Locator, type Page, expect, test} from '@playwright/test';

import {
    AUTOMATION_API_PATH,
    DEVELOPMENT_ENVIRONMENT_ID,
    type GraphqlResponseI,
    apiFetch,
    apiJson,
    graphqlData,
    graphqlRequest,
} from './apiUtils';
import {
    type WorkflowDefinitionType,
    deleteConnection,
    getProjectVersionWorkflows,
    importWorkflowDefinition,
    publishProject,
} from './deploymentUtils';
import getRandomString from './getRandomString';

export const STAGING_ENVIRONMENT_ID = 1;

export const STAGING_LABEL = 'STAGING';

const API_PLATFORM_API_PATH = '/api/automation/api-platform/internal';

export type PromotionResourceType = 'A2A_SERVER' | 'API_COLLECTION' | 'MCP_SERVER' | 'PROJECT_DEPLOYMENT';

export interface PromotionPreviewI {
    connections: Array<{
        sourceConnectionId: string;
        sourceConnectionName: string;
        suggestedTargetConnectionId: string | null;
        usedBy: string[];
    }>;
    existingTargetId: string | null;
    existingTargetName: string | null;
}

export interface PromotionResultI {
    created: boolean;
    targetId: string;
    targetUrl: string | null;
    unresolvedConnectionIds: string[];
}

const ENVIRONMENTS_QUERY = 'query environments { environments { id name } }';

const PROMOTION_PREVIEW_QUERY = `query environmentPromotionPreview(
    $resourceType: PromotionResourceType!, $sourceId: ID!, $targetEnvironmentId: ID!
) {
    environmentPromotionPreview(
        resourceType: $resourceType, sourceId: $sourceId, targetEnvironmentId: $targetEnvironmentId
    ) {
        connections { sourceConnectionId sourceConnectionName suggestedTargetConnectionId usedBy }
        existingTargetId
        existingTargetName
    }
}`;

export const PROMOTE_TO_ENVIRONMENT_MUTATION = `mutation promoteToEnvironment($input: PromoteToEnvironmentInput!) {
    promoteToEnvironment(input: $input) { created targetId targetUrl unresolvedConnectionIds }
}`;

/**
 * Promotion is an Enterprise feature that also needs a second environment to promote into, so a CE server or a
 * single-environment server skips the promotion specs instead of failing them.
 */
export async function skipUnlessPromotionIsAvailable(page: Page): Promise<void> {
    const promotionTypeResponse = await graphqlRequest<{__type: {name: string} | null}>(
        page,
        'query { __type(name: "PromotionResourceType") { name } }'
    );

    test.skip(!promotionTypeResponse.data?.__type, 'Environment promotion needs an EE server');

    const {environments} = await graphqlData<{environments: Array<{id: string}>}>(page, ENVIRONMENTS_QUERY);

    test.skip(
        !environments.some((environment) => Number(environment.id) === STAGING_ENVIRONMENT_ID),
        'Environment promotion needs a Staging environment'
    );
}

export async function getPromotionPreview(
    page: Page,
    resourceType: PromotionResourceType,
    sourceId: number | string
): Promise<PromotionPreviewI> {
    const data = await graphqlData<{environmentPromotionPreview: PromotionPreviewI}>(page, PROMOTION_PREVIEW_QUERY, {
        resourceType,
        sourceId: String(sourceId),
        targetEnvironmentId: String(STAGING_ENVIRONMENT_ID),
    });

    return data.environmentPromotionPreview;
}

export async function promoteViaApi(
    page: Page,
    resourceType: PromotionResourceType,
    sourceId: number | string
): Promise<GraphqlResponseI<{promoteToEnvironment: PromotionResultI}>> {
    return graphqlRequest<{promoteToEnvironment: PromotionResultI}>(page, PROMOTE_TO_ENVIRONMENT_MUTATION, {
        input: {
            connectionMappings: [],
            resourceType,
            sourceId: String(sourceId),
            targetEnvironmentId: String(STAGING_ENVIRONMENT_ID),
        },
    });
}

export function buildSlackWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [
            {
                name: 'slack_1',
                parameters: {channel: 'C0123456789', text: 'Sent by Playwright'},
                type: 'slack/v1/sendChannelMessage',
            },
        ],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

export function buildPlainWorkflow(label: string): WorkflowDefinitionType {
    return {
        description: label,
        label,
        tasks: [{name: 'var_1', parameters: {type: 'STRING', value: 'promoted'}, type: 'var/v1/set'}],
        triggers: [{name: 'trigger_1', parameters: {}, type: 'webhook/v1/autoRespondWithHTTP200'}],
    };
}

interface CreateSlackConnectionProps {
    environmentId: number;
    name: string;
    page: Page;
    workspaceId: number;
}

export async function createSlackConnection({
    environmentId,
    name,
    page,
    workspaceId,
}: CreateSlackConnectionProps): Promise<number> {
    return apiJson<number>({
        body: {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            componentName: 'slack',
            connectionVersion: 1,
            environmentId,
            name,
            parameters: {
                access_token: '',
                clientId: 'playwright-client-id',
                clientSecret: 'playwright-client-secret',
                signingSecret: 'playwright-signing-secret',
            },
            workspaceId,
        },
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/connections`,
    });
}

export interface ProjectDeploymentI {
    enabled: boolean;
    environmentId?: number;
    id: number;
    name: string;
    projectDeploymentWorkflows: Array<{connections?: Array<{connectionId?: number}>}>;
    projectVersion: number;
}

export async function getProjectDeployment(page: Page, deploymentId: number | string): Promise<ProjectDeploymentI> {
    return apiJson<ProjectDeploymentI>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}`,
    });
}

interface CreateDevelopmentDeploymentProps {
    connectionId?: number;
    page: Page;
    projectId: string;
    workflow: WorkflowDefinitionType;
}

export interface DevelopmentDeploymentI {
    deploymentId: number;
    deploymentName: string;
    projectVersion: number;
}

/**
 * Imports one workflow into the project, publishes it and deploys it in Development, binding every connection of
 * the workflow to the given connection.
 */
export async function createDevelopmentDeployment({
    connectionId,
    page,
    projectId,
    workflow,
}: CreateDevelopmentDeploymentProps): Promise<DevelopmentDeploymentI> {
    await importWorkflowDefinition(page, projectId, workflow);

    const projectVersion = await publishProject(page, projectId);

    const versionWorkflows = await getProjectVersionWorkflows(page, projectId, projectVersion);

    const deploymentName = `deployment_${getRandomString()}`;

    const deploymentId = await apiJson<number>({
        body: {
            enabled: false,
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            name: deploymentName,
            projectDeploymentWorkflows: versionWorkflows.map((versionWorkflow) => ({
                connections: [...(versionWorkflow.tasks ?? []), ...(versionWorkflow.triggers ?? [])]
                    .flatMap((workflowNode) => workflowNode.connections ?? [])
                    .filter(() => connectionId != null)
                    .map((workflowConnection) => ({
                        connectionId,
                        workflowConnectionKey: workflowConnection.key,
                        workflowNodeName: workflowConnection.workflowNodeName,
                    })),
                enabled: true,
                inputs: {},
                workflowId: versionWorkflow.id,
            })),
            projectId: Number(projectId),
            projectVersion,
        },
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments`,
    });

    return {deploymentId, deploymentName, projectVersion};
}

export async function getWorkspaceProjectDeployments(
    page: Page,
    workspaceId: number,
    projectId: string,
    environmentId: number
): Promise<ProjectDeploymentI[]> {
    return apiJson<ProjectDeploymentI[]>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/workspaces/${workspaceId}/project-deployments?environmentId=${environmentId}&projectId=${projectId}`,
    });
}

export async function deleteProjectDeploymentQuietly(page: Page, deploymentId: number | string): Promise<void> {
    await apiFetch({
        method: 'PATCH',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}/enable/false`,
    });

    await apiFetch({method: 'DELETE', page, path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}`});
}

export async function deleteConnectionQuietly(page: Page, connectionId: number): Promise<void> {
    try {
        await deleteConnection(page, connectionId);
    } catch (error) {
        console.warn(`Failed to delete connection ${connectionId}:`, error);
    }
}

interface CreateApiCollectionProps {
    name: string;
    page: Page;
    projectId: string;
    projectVersion: number;
    workspaceId: number;
}

export async function createApiCollection({
    name,
    page,
    projectId,
    projectVersion,
    workspaceId,
}: CreateApiCollectionProps): Promise<number> {
    const apiCollection = await apiJson<{id: number}>({
        body: {
            contextPath: `playwright-${getRandomString()}`,
            enabled: false,
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            name,
            projectId: Number(projectId),
            projectVersion,
            workspaceId,
        },
        method: 'POST',
        page,
        path: `${API_PLATFORM_API_PATH}/api-collections`,
    });

    return apiCollection.id;
}

export async function getApiCollection(page: Page, apiCollectionId: number | string): Promise<{enabled: boolean}> {
    return apiJson<{enabled: boolean}>({
        method: 'GET',
        page,
        path: `${API_PLATFORM_API_PATH}/api-collections/${apiCollectionId}`,
    });
}

export async function deleteApiCollectionQuietly(page: Page, apiCollectionId: number | string): Promise<void> {
    await apiFetch({method: 'DELETE', page, path: `${API_PLATFORM_API_PATH}/api-collections/${apiCollectionId}`});
}

export async function createWorkspaceMcpServer(page: Page, workspaceId: number, name: string): Promise<string> {
    const data = await graphqlData<{createWorkspaceMcpServer: {id: string}}>(
        page,
        `mutation createWorkspaceMcpServer($input: CreateWorkspaceMcpServerInput!) {
            createWorkspaceMcpServer(input: $input) { id }
        }`,
        {
            input: {
                authenticationRequired: false,
                enabled: true,
                environmentId: String(DEVELOPMENT_ENVIRONMENT_ID),
                name,
                type: 'AUTOMATION',
                workspaceId: String(workspaceId),
            },
        }
    );

    return data.createWorkspaceMcpServer.id;
}

export async function deleteWorkspaceMcpServerQuietly(page: Page, mcpServerId: string): Promise<void> {
    await graphqlRequest(
        page,
        'mutation deleteWorkspaceMcpServer($id: ID!) { deleteWorkspaceMcpServer(mcpServerId: $id) }',
        {
            id: mcpServerId,
        }
    );
}

export async function createA2aServer(page: Page, workspaceId: number, name: string): Promise<string> {
    const data = await graphqlData<{createA2aServer: {id: string}}>(
        page,
        `mutation createA2aServer($input: CreateA2aServerInput!) { createA2aServer(input: $input) { id } }`,
        {
            input: {
                authenticationRequired: false,
                environmentId: String(DEVELOPMENT_ENVIRONMENT_ID),
                name,
                workspaceId: String(workspaceId),
            },
        }
    );

    return data.createA2aServer.id;
}

export async function deleteA2aServerQuietly(page: Page, a2aServerId: string): Promise<void> {
    await graphqlRequest(page, 'mutation deleteA2aServer($id: ID!) { deleteA2aServer(id: $id) }', {id: a2aServerId});
}

/**
 * Opens the row's three-dot menu and returns the Promote to environment item, whether or not it is rendered.
 */
export async function openRowMenu(page: Page, rowName: string): Promise<Locator> {
    const nameText = page.getByText(rowName, {exact: true}).first();

    await expect(nameText).toBeVisible({timeout: 30000});

    const row = page.locator('div.cursor-pointer, li, [role="presentation"]').filter({has: nameText}).last();

    await row.locator('button[aria-haspopup="menu"]').last().click();

    await expect(page.getByRole('menu')).toBeVisible();

    return page.getByRole('menuitem', {name: 'Promote to environment…'});
}

export async function openPromotionDialog(page: Page, rowName: string): Promise<Locator> {
    const promoteMenuItem = await openRowMenu(page, rowName);

    await promoteMenuItem.click();

    const dialog = page.getByRole('dialog', {name: `Promote "${rowName}"`});

    await expect(dialog).toBeVisible();

    await expect(dialog.getByRole('combobox', {name: 'Target environment'})).toHaveText(STAGING_LABEL);

    await expect(dialog.getByText('Loading preview…')).toBeHidden({timeout: 15000});

    return dialog;
}

export async function promoteAndWaitForToast(page: Page, dialog: Locator): Promise<Locator> {
    await dialog.getByRole('button', {exact: true, name: 'Promote'}).click();

    await expect(dialog).toBeHidden({timeout: 30000});

    const toast = page.locator('[data-sonner-toast]').filter({hasText: `Promoted to ${STAGING_LABEL}`});

    await expect(toast).toBeVisible();

    return toast;
}

export async function setCurrentEnvironment(page: Page, environmentId: number): Promise<void> {
    await page.evaluate((currentEnvironmentId) => {
        const storedState = JSON.parse(localStorage.getItem('bytechef.environment') ?? '{"state":{},"version":0}');

        storedState.state = {...storedState.state, currentEnvironmentId};

        localStorage.setItem('bytechef.environment', JSON.stringify(storedState));
    }, environmentId);
}

export async function getCurrentEnvironmentId(page: Page): Promise<number | undefined> {
    return page.evaluate(
        () => JSON.parse(localStorage.getItem('bytechef.environment') ?? '{}')?.state?.currentEnvironmentId
    );
}
