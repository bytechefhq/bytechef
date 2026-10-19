import {type Page} from '@playwright/test';

import {AUTOMATION_API_PATH, DEVELOPMENT_ENVIRONMENT_ID, apiFetch, apiJson, toServerUrl} from './apiUtils';
import {CHANNEL_SECRETS, getDiscordPublicKeyHex} from './channelCallbackUtils';
import getRandomString from './getRandomString';

export type WorkflowDefinitionType = Record<string, unknown> & {label: string};

export type ConnectionComponentNameType =
    'discord' | 'email' | 'mattermost' | 'openAi' | 'slack' | 'telegram' | 'whatsApp';

interface ConnectionTemplateI {
    authorizationType?: string;
    parameters: () => Record<string, unknown>;
}

const CONNECTION_TEMPLATES: Record<ConnectionComponentNameType, ConnectionTemplateI> = {
    discord: {
        authorizationType: 'BEARER_TOKEN',
        parameters: () => ({publicKey: getDiscordPublicKeyHex(), token: 'playwright-fake-discord-token'}),
    },
    email: {
        parameters: () => ({cryptoProtocol: 'NONE', host: 'localhost'}),
    },
    mattermost: {
        authorizationType: 'BEARER_TOKEN',
        parameters: () => ({domain: '127.0.0.1:1', token: 'playwright-fake-mattermost-token'}),
    },
    openAi: {
        authorizationType: 'BEARER_TOKEN',
        parameters: () => ({token: process.env.OPENAI_API_KEY ?? ''}),
    },
    slack: {
        authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
        parameters: () => ({
            access_token: '',
            clientId: 'playwright-client-id',
            clientSecret: 'playwright-client-secret',
            signingSecret: CHANNEL_SECRETS.slackSigningSecret,
        }),
    },
    telegram: {
        authorizationType: 'CUSTOM',
        parameters: () => ({
            botToken: '123456:playwright-fake-bot-token',
            webhookSecretToken: CHANNEL_SECRETS.telegramWebhookSecretToken,
        }),
    },
    whatsApp: {
        authorizationType: 'CUSTOM',
        parameters: () => ({
            appSecret: CHANNEL_SECRETS.whatsAppAppSecret,
            phoneNumberId: '111',
            systemUserAccessToken: 'playwright-fake-whatsapp-token',
        }),
    },
};

export function isConnectionComponentName(componentName: string): componentName is ConnectionComponentNameType {
    return componentName in CONNECTION_TEMPLATES;
}

export async function getProjectWorkspaceId(page: Page, projectId: string): Promise<number> {
    const project = await apiJson<{workspaceId: number}>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}`,
    });

    return project.workspaceId;
}

export async function createProjectViaApi(page: Page, workspaceId: number, projectName?: string): Promise<string> {
    const projectId = await apiJson<number>({
        body: {name: projectName ?? `project_${getRandomString()}`, workspaceId},
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/projects`,
    });

    return String(projectId);
}

export async function deleteProjectViaApi(page: Page, projectId: string): Promise<void> {
    await apiFetch({method: 'DELETE', page, path: `${AUTOMATION_API_PATH}/projects/${projectId}`});
}

export async function createConnection(
    page: Page,
    workspaceId: number,
    componentName: ConnectionComponentNameType,
    parameterOverrides: Record<string, unknown> = {}
): Promise<number> {
    const connectionTemplate = CONNECTION_TEMPLATES[componentName];

    return apiJson<number>({
        body: {
            authorizationType: connectionTemplate.authorizationType,
            componentName,
            connectionVersion: 1,
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            name: `playwright-${componentName}-${getRandomString()}`,
            parameters: {...connectionTemplate.parameters(), ...parameterOverrides},
            workspaceId,
        },
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/connections`,
    });
}

export async function deleteConnection(page: Page, connectionId: number): Promise<void> {
    await apiFetch({method: 'DELETE', page, path: `${AUTOMATION_API_PATH}/connections/${connectionId}`});
}

export async function importWorkflowDefinition(
    page: Page,
    projectId: string,
    definition: WorkflowDefinitionType
): Promise<void> {
    await apiJson({
        body: {definition: JSON.stringify(definition)},
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}/workflows`,
    });
}

export interface ProjectWorkflowI {
    id: string;
    label: string;
    projectWorkflowId: number;
    tasks?: Array<{connections?: WorkflowConnectionI[]}>;
    triggers?: Array<{connections?: WorkflowConnectionI[]}>;
    workflowUuid: string;
}

interface WorkflowConnectionI {
    componentName: string;
    key: string;
    workflowNodeName: string;
}

export async function getProjectWorkflows(page: Page, projectId: string): Promise<ProjectWorkflowI[]> {
    return apiJson<ProjectWorkflowI[]>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}/workflows`,
    });
}

export async function publishProject(page: Page, projectId: string): Promise<number> {
    await apiJson({
        body: {description: 'Published by Playwright'},
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}/publish`,
    });

    const project = await apiJson<{lastPublishedProjectVersion?: {version: number}; lastProjectVersion: number}>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}`,
    });

    return project.lastPublishedProjectVersion?.version ?? project.lastProjectVersion - 1;
}

export interface DeployedWorkflowI {
    label: string;
    projectWorkflowId: number;
    webhookUrl: string;
    workflowId: string;
    workflowUuid: string;
}

export interface DeployedProjectI {
    deploymentId: number;
    projectId: string;
    projectVersion: number;
    workflows: DeployedWorkflowI[];
}

interface DeployProjectProps {
    connectionIdResolver: (componentName: ConnectionComponentNameType) => Promise<number>;
    enabledWorkflowLabels?: string[];
    page: Page;
    projectId: string;
}

export async function deployProject({
    connectionIdResolver,
    enabledWorkflowLabels,
    page,
    projectId,
}: DeployProjectProps): Promise<DeployedProjectI> {
    const projectVersion = await publishProject(page, projectId);

    const versionWorkflows = await getProjectVersionWorkflows(page, projectId, projectVersion);

    const projectDeploymentWorkflows = [];

    for (const versionWorkflow of versionWorkflows) {
        const workflowConnections = [...(versionWorkflow.tasks ?? []), ...(versionWorkflow.triggers ?? [])].flatMap(
            (workflowNode) => workflowNode.connections ?? []
        );

        const connections = [];

        for (const workflowConnection of workflowConnections) {
            if (isConnectionComponentName(workflowConnection.componentName)) {
                connections.push({
                    connectionId: await connectionIdResolver(workflowConnection.componentName),
                    workflowConnectionKey: workflowConnection.key,
                    workflowNodeName: workflowConnection.workflowNodeName,
                });
            }
        }

        projectDeploymentWorkflows.push({
            connections,
            enabled: !enabledWorkflowLabels || enabledWorkflowLabels.includes(versionWorkflow.label),
            inputs: {},
            workflowId: versionWorkflow.id,
        });
    }

    const deploymentId = await apiJson<number>({
        body: {
            enabled: false,
            environmentId: DEVELOPMENT_ENVIRONMENT_ID,
            name: `deployment_${getRandomString()}`,
            projectDeploymentWorkflows,
            projectId: Number(projectId),
            projectVersion,
        },
        method: 'POST',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments`,
    });

    await apiJson({
        method: 'PATCH',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}/enable/true`,
    });

    const deployment = await apiJson<{
        projectDeploymentWorkflows: Array<{staticWebhookUrl?: string; workflowId: string; workflowUuid: string}>;
    }>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}`,
    });

    const workflows = deployment.projectDeploymentWorkflows.map((deploymentWorkflow) => {
        const versionWorkflow = versionWorkflows.find(
            (projectWorkflow) => projectWorkflow.id === deploymentWorkflow.workflowId
        );

        return {
            label: versionWorkflow?.label ?? '',
            projectWorkflowId: versionWorkflow?.projectWorkflowId ?? 0,
            webhookUrl: deploymentWorkflow.staticWebhookUrl ? toServerUrl(deploymentWorkflow.staticWebhookUrl) : '',
            workflowId: deploymentWorkflow.workflowId,
            workflowUuid: deploymentWorkflow.workflowUuid,
        };
    });

    return {deploymentId, projectId, projectVersion, workflows};
}

export async function deleteDeployment(page: Page, deploymentId: number): Promise<void> {
    await apiFetch({
        method: 'PATCH',
        page,
        path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}/enable/false`,
    });

    await apiFetch({method: 'DELETE', page, path: `${AUTOMATION_API_PATH}/project-deployments/${deploymentId}`});
}

export async function getProjectVersionWorkflows(
    page: Page,
    projectId: string,
    projectVersion: number
): Promise<ProjectWorkflowI[]> {
    return apiJson<ProjectWorkflowI[]>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/projects/${projectId}/versions/${projectVersion}/workflows`,
    });
}
