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

const EMBEDDED_API_PATH = '/api/embedded/internal';

export type PromotionResourceType =
    | 'A2A_SERVER'
    | 'API_COLLECTION'
    | 'AUTOMATION_CONFIGURATION'
    | 'DATA_TABLE'
    | 'INTEGRATION_INSTANCE_CONFIGURATION'
    | 'MCP_SERVER'
    | 'PROJECT_DEPLOYMENT';

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
    $resourceType: PromotionResourceType!, $sourceEnvironmentId: ID!, $sourceId: ID!, $targetEnvironmentId: ID!
) {
    environmentPromotionPreview(
        resourceType: $resourceType, sourceEnvironmentId: $sourceEnvironmentId, sourceId: $sourceId,
        targetEnvironmentId: $targetEnvironmentId
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
        sourceEnvironmentId: String(DEVELOPMENT_ENVIRONMENT_ID),
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
            sourceEnvironmentId: String(DEVELOPMENT_ENVIRONMENT_ID),
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

export interface McpServerComponentI {
    id: string;
    mcpTools: Array<{name: string}>;
    version: number;
}

export async function createMcpComponentWithTools(page: Page, mcpServerId: string, toolNames: string[]): Promise<void> {
    await graphqlData(
        page,
        `mutation createMcpComponentWithTools($input: McpComponentWithToolsInput!) {
            createMcpComponentWithTools(input: $input) { id }
        }`,
        {
            input: {
                componentName: 'var',
                componentVersion: 1,
                mcpServerId,
                tools: toolNames.map((name) => ({name, parameters: {}})),
                version: 0,
            },
        }
    );
}

export async function getMcpServerComponent(page: Page, mcpServerId: string): Promise<McpServerComponentI> {
    const data = await graphqlData<{mcpComponentsByServerId: McpServerComponentI[]}>(
        page,
        `query mcpComponentsByServerId($mcpServerId: ID!) {
            mcpComponentsByServerId(mcpServerId: $mcpServerId) { id version mcpTools { name } }
        }`,
        {mcpServerId}
    );

    return data.mcpComponentsByServerId[0];
}

export async function setMcpComponentTools(
    page: Page,
    mcpServerId: string,
    mcpComponent: McpServerComponentI,
    toolNames: string[]
): Promise<void> {
    await graphqlData(
        page,
        `mutation updateMcpComponentWithTools($id: ID!, $input: McpComponentWithToolsInput!) {
            updateMcpComponentWithTools(id: $id, input: $input) { id }
        }`,
        {
            id: mcpComponent.id,
            input: {
                componentName: 'var',
                componentVersion: 1,
                mcpServerId,
                tools: toolNames.map((name) => ({name, parameters: {}})),
                version: mcpComponent.version,
            },
        }
    );
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

export interface DataTableColumnI {
    id: string;
    name: string;
    type: string;
}

export interface DataTableI {
    baseName: string;
    columns: DataTableColumnI[];
    id: string;
}

export interface DataTableColumnInputI {
    name: string;
    type: 'BOOLEAN' | 'DATE' | 'DATE_TIME' | 'INTEGER' | 'NUMBER' | 'STRING';
}

export async function listDataTables(page: Page, workspaceId: number, environmentId: number): Promise<DataTableI[]> {
    const data = await graphqlData<{dataTables: DataTableI[]}>(
        page,
        `query dataTables($environmentId: ID!, $workspaceId: ID!) {
            dataTables(environmentId: $environmentId, workspaceId: $workspaceId) {
                id baseName columns { id name type }
            }
        }`,
        {environmentId: String(environmentId), workspaceId: String(workspaceId)}
    );

    return data.dataTables;
}

export async function findDataTable(
    page: Page,
    workspaceId: number,
    environmentId: number,
    baseName: string
): Promise<DataTableI | undefined> {
    const dataTables = await listDataTables(page, workspaceId, environmentId);

    return dataTables.find((dataTable) => dataTable.baseName === baseName);
}

export async function createDataTable(
    page: Page,
    workspaceId: number,
    baseName: string,
    columns: DataTableColumnInputI[]
): Promise<string> {
    await graphqlData(
        page,
        'mutation createDataTable($input: CreateDataTableInput!) { createDataTable(input: $input) }',
        {
            input: {
                baseName,
                columns,
                environmentId: String(DEVELOPMENT_ENVIRONMENT_ID),
                workspaceId: String(workspaceId),
            },
        }
    );

    const dataTable = await findDataTable(page, workspaceId, DEVELOPMENT_ENVIRONMENT_ID, baseName);

    if (!dataTable) {
        throw new Error(`Data table ${baseName} was not created`);
    }

    return dataTable.id;
}

export async function addDataTableColumn(
    page: Page,
    environmentId: number,
    tableId: string,
    column: DataTableColumnInputI
): Promise<void> {
    await graphqlData(
        page,
        'mutation addDataTableColumn($input: AddColumnInput!) { addDataTableColumn(input: $input) }',
        {
            input: {column, environmentId: String(environmentId), tableId},
        }
    );
}

export async function removeDataTableColumn(
    page: Page,
    environmentId: number,
    tableId: string,
    columnId: string
): Promise<void> {
    await graphqlData(
        page,
        'mutation removeDataTableColumn($input: RemoveColumnInput!) { removeDataTableColumn(input: $input) }',
        {input: {columnId, environmentId: String(environmentId), tableId}}
    );
}

export async function insertDataTableRow(
    page: Page,
    environmentId: number,
    tableId: string,
    values: Record<string, unknown>
): Promise<void> {
    await graphqlData(
        page,
        'mutation insertDataTableRow($input: InsertRowInput!) { insertDataTableRow(input: $input) { id } }',
        {
            input: {environmentId: String(environmentId), tableId, values},
        }
    );
}

export async function countDataTableRows(page: Page, environmentId: number, tableId: string): Promise<number> {
    const data = await graphqlData<{dataTableRows: Array<{id: string}>}>(
        page,
        'query dataTableRows($environmentId: ID!, $tableId: ID!) { dataTableRows(environmentId: $environmentId, tableId: $tableId) { id } }',
        {environmentId: String(environmentId), tableId}
    );

    return data.dataTableRows.length;
}

export async function dropDataTableQuietly(page: Page, environmentId: number, tableId: string): Promise<void> {
    await graphqlRequest(page, 'mutation dropDataTable($input: RemoveTableInput!) { dropDataTable(input: $input) }', {
        input: {environmentId: String(environmentId), tableId},
    });
}

export interface EmbeddedIntegrationWorkflowI {
    id: string;
    workflowUuid?: string;
}

export interface EmbeddedIntegrationI {
    id: number;
    version: number;
    workflows: EmbeddedIntegrationWorkflowI[];
}

export function buildInputWorkflowDefinition(label: string): string {
    return JSON.stringify({
        description: label,
        inputs: [{internalOnly: true, label: 'Greeting', name: 'greeting', required: false, type: 'string'}],
        label,
        tasks: [{label: 'Var', name: 'var_1', parameters: {type: 'STRING', value: 'hello'}, type: 'var/v1/set'}],
        triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
    });
}

export async function createPublishedEmbeddedIntegration(page: Page, name: string): Promise<EmbeddedIntegrationI> {
    const integrationId = await apiJson<number>({
        body: {componentName: 'slack', multipleInstances: true, name},
        method: 'POST',
        page,
        path: `${EMBEDDED_API_PATH}/integrations`,
    });

    await apiJson<number>({
        body: {definition: buildInputWorkflowDefinition(name)},
        method: 'POST',
        page,
        path: `${EMBEDDED_API_PATH}/integrations/${integrationId}/workflows`,
    });

    await apiFetch({
        body: {description: 'Playwright'},
        method: 'POST',
        page,
        path: `${EMBEDDED_API_PATH}/integrations/${integrationId}/publish`,
    });

    const versions = await apiJson<Array<{status?: string; version: number}>>({
        method: 'GET',
        page,
        path: `${EMBEDDED_API_PATH}/integrations/${integrationId}/versions`,
    });

    const version = Math.max(
        ...versions
            .filter((integrationVersion) => integrationVersion.status === 'PUBLISHED')
            .map((integrationVersion) => integrationVersion.version)
    );

    const workflows = await apiJson<EmbeddedIntegrationWorkflowI[]>({
        method: 'GET',
        page,
        path: `${EMBEDDED_API_PATH}/integrations/${integrationId}/versions/${version}/workflows`,
    });

    return {id: integrationId, version, workflows};
}

export async function deleteEmbeddedIntegrationQuietly(page: Page, integrationId: number): Promise<void> {
    await apiFetch({method: 'DELETE', page, path: `${EMBEDDED_API_PATH}/integrations/${integrationId}`});
}

export interface EmbeddedConfigurationWorkflowI {
    enabled?: boolean;
    inputs?: Record<string, unknown>;
    workflowId?: string;
}

export interface EmbeddedIntegrationInstanceConfigurationI {
    connectionAuthorizationParameters?: Record<string, unknown>;
    connectionConnectionParameters?: Record<string, unknown>;
    connectionParameters?: Record<string, unknown>;
    enabled?: boolean;
    id: number;
    integrationInstanceConfigurationWorkflows?: EmbeddedConfigurationWorkflowI[];
    name: string;
}

export async function createIntegrationInstanceConfiguration(
    page: Page,
    integration: EmbeddedIntegrationI,
    name: string,
    inputs: Record<string, unknown>
): Promise<number> {
    const configurationId = await apiJson<number>({
        body: {
            connectionParameters: {clientId: 'playwright-client-id', clientSecret: 'playwright-client-secret'},
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            integrationId: integration.id,
            integrationInstanceConfigurationWorkflows: integration.workflows.map((workflow) => ({
                enabled: true,
                inputs,
                workflowId: workflow.id,
            })),
            integrationVersion: integration.version,
            name,
        },
        method: 'POST',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations`,
    });

    await apiJson<void>({
        method: 'PATCH',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations/${configurationId}/enable/true`,
    });

    return configurationId;
}

export async function findIntegrationInstanceConfiguration(
    page: Page,
    environmentId: number,
    name: string
): Promise<EmbeddedIntegrationInstanceConfigurationI | undefined> {
    const configurations = await apiJson<EmbeddedIntegrationInstanceConfigurationI[]>({
        method: 'GET',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations?environmentId=${environmentId}`,
    });

    return configurations.find((configuration) => configuration.name === name);
}

export async function deleteIntegrationInstanceConfigurationQuietly(
    page: Page,
    configurationId: number
): Promise<void> {
    await apiFetch({
        method: 'DELETE',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations/${configurationId}`,
    });
}

export async function updateIntegrationInstanceConfigurationInputs(
    page: Page,
    configurationId: number,
    inputs: Record<string, unknown>
): Promise<void> {
    const configuration = await apiJson<EmbeddedIntegrationInstanceConfigurationI>({
        method: 'GET',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations/${configurationId}`,
    });

    const workflows = configuration.integrationInstanceConfigurationWorkflows ?? [];

    if (workflows.length === 0) {
        throw new Error(`Integration instance configuration ${configurationId} has no workflows to update`);
    }

    await apiJson<void>({
        body: {
            ...configuration,
            integrationInstanceConfigurationWorkflows: workflows.map((workflow) => ({...workflow, inputs})),
        },
        method: 'PUT',
        page,
        path: `${EMBEDDED_API_PATH}/integration-instance-configurations/${configurationId}`,
    });
}

export function buildEmbeddedSlackWorkflowDefinition(label: string): string {
    return JSON.stringify({
        description: label,
        inputs: [{internalOnly: true, label: 'Greeting', name: 'greeting', required: false, type: 'string'}],
        label,
        tasks: [
            {
                label: 'Send',
                name: 'slack_1',
                parameters: {channel: 'C0123456789', text: 'Sent by Playwright'},
                type: 'slack/v1/sendChannelMessage',
            },
        ],
        triggers: [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}],
    });
}

export async function createEmbeddedSlackConnection(page: Page, environmentId: number, name: string): Promise<number> {
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
        },
        method: 'POST',
        page,
        path: `${EMBEDDED_API_PATH}/connections`,
    });
}

export async function deleteEmbeddedConnectionQuietly(page: Page, connectionId: number): Promise<void> {
    await apiFetch({method: 'DELETE', page, path: `${EMBEDDED_API_PATH}/connections/${connectionId}`});
}

export async function updateAutomationConfigurationWorkflow(
    page: Page,
    configurationId: string,
    workflowUuid: string,
    providedConnections: Record<string, number>,
    providedInputs: Record<string, unknown>
): Promise<void> {
    await graphqlData(
        page,
        `mutation updateAutomationProjectConfigurationWorkflow(
            $id: ID!, $workflowUuid: ID!, $providedConnections: Map, $providedInputs: Map
        ) {
            updateAutomationProjectConfigurationWorkflow(
                id: $id, workflowUuid: $workflowUuid, providedConnections: $providedConnections,
                providedInputs: $providedInputs
            )
        }`,
        {id: configurationId, providedConnections, providedInputs, workflowUuid}
    );
}

export interface AutomationConfigurationWorkflowI {
    enabled: boolean;
    providedConnections: unknown;
    providedInputs: unknown;
    workflowUuid: string;
}

export interface AutomationConfigurationI {
    enabled: boolean;
    environmentId: string;
    id: string;
    name: string;
    projectVersion: number;
    workflows: AutomationConfigurationWorkflowI[];
}

export async function createPublishedAutomationProject(
    page: Page,
    name: string,
    workflowDefinition: string
): Promise<string> {
    const {createAutomationProject: projectId} = await graphqlData<{createAutomationProject: string}>(
        page,
        'mutation createAutomationProject($name: String!) { createAutomationProject(name: $name) }',
        {name}
    );

    await graphqlData(
        page,
        `mutation createAutomationProjectWorkflow($projectId: ID!, $definition: String) {
            createAutomationProjectWorkflow(projectId: $projectId, definition: $definition)
        }`,
        {definition: workflowDefinition, projectId}
    );

    await graphqlData(page, 'mutation publishAutomationProject($id: ID!) { publishAutomationProject(id: $id) }', {
        id: projectId,
    });

    return projectId;
}

export async function createAutomationConfiguration(page: Page, projectId: string, name: string): Promise<string> {
    const {createAutomationProjectConfiguration: configurationId} = await graphqlData<{
        createAutomationProjectConfiguration: string;
    }>(
        page,
        `mutation createAutomationProjectConfiguration($projectId: ID!, $environmentId: ID!, $name: String!) {
            createAutomationProjectConfiguration(
                projectId: $projectId, environmentId: $environmentId, name: $name, projectVersion: 1,
                enabled: true, automationHubVisible: true
            )
        }`,
        {environmentId: String(DEVELOPMENT_ENVIRONMENT_ID), name, projectId}
    );

    return configurationId;
}

export async function findAutomationConfiguration(
    page: Page,
    projectId: string,
    environmentId: number,
    name: string
): Promise<AutomationConfigurationI | undefined> {
    const data = await graphqlData<{automationProjectConfigurations: AutomationConfigurationI[]}>(
        page,
        `query automationProjectConfigurations($projectId: ID!, $environmentId: ID) {
            automationProjectConfigurations(projectId: $projectId, environmentId: $environmentId) {
                id environmentId name enabled projectVersion
                workflows { workflowUuid enabled providedConnections providedInputs }
            }
        }`,
        {environmentId: String(environmentId), projectId}
    );

    return data.automationProjectConfigurations.find((configuration) => configuration.name === name);
}

export async function deleteAutomationConfigurationQuietly(page: Page, configurationId: string): Promise<void> {
    await graphqlRequest(
        page,
        'mutation deleteAutomationProjectConfiguration($id: ID!) { deleteAutomationProjectConfiguration(id: $id) }',
        {
            id: configurationId,
        }
    );
}

export async function deleteAutomationProjectQuietly(page: Page, projectId: string): Promise<void> {
    await graphqlRequest(page, 'mutation deleteAutomationProject($id: ID!) { deleteAutomationProject(id: $id) }', {
        id: projectId,
    });
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

export async function goToNextPromotionStep(dialog: Locator): Promise<void> {
    await dialog.getByRole('button', {exact: true, name: 'Next'}).click();
}

export async function walkToPromoteButton(dialog: Locator): Promise<Locator> {
    const nextButton = dialog.getByRole('button', {exact: true, name: 'Next'});
    const promoteButton = dialog.getByRole('button', {exact: true, name: 'Promote'});

    await expect(promoteButton.or(nextButton)).toBeVisible();

    while (!(await promoteButton.isVisible())) {
        await nextButton.click();

        await expect(promoteButton.or(nextButton)).toBeVisible();
    }

    const acknowledgement = dialog.getByRole('checkbox', {name: /I understand \d+ change\(s\) delete or overwrite/});

    if ((await acknowledgement.count()) > 0) {
        await acknowledgement.check();
    }

    await expect(promoteButton).toBeEnabled();

    return promoteButton;
}

export async function promoteThroughWizard(dialog: Locator): Promise<void> {
    const promoteButton = await walkToPromoteButton(dialog);

    await promoteButton.click();
}

export async function promoteAndWaitForToast(page: Page, dialog: Locator): Promise<Locator> {
    await promoteThroughWizard(dialog);

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
