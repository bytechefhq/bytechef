import {type Locator, type Page, type Request, type Route, expect} from '@playwright/test';

import {AI_HUB_ATTACH_URL, type AgUiTurnType, toSseBody} from './agUiStreamStub';
import {
    type AiProviderCatalogType,
    ENABLED_PROVIDER_CATALOG,
    hideReactQueryDevtools,
    readGraphQlOperationName,
    stubAiProviders,
} from './copilotUtils';
import getRandomString from './getRandomString';

export const AI_HUB_PATH = '/automation/ai-hub';

export const aiHubChatPath = (chatId: number) => `${AI_HUB_PATH}/chats/${chatId}`;

export const TURN_IN_FLIGHT_TOAST = 'A turn is already running in this chat.';

export const EDIT_NOT_SENT_TOAST =
    'The chat changed while your edit was being saved, so the edited message was not sent. Open the chat to send it again.';

const AI_HUB_CHAT_FIELDS = `
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    autoTitled
    ownerUserId
    isOwner
`;

interface AiApplicationInfoI {
    copilotEnabled?: boolean;
    hubEnabled?: boolean;
}

export async function overrideAiApplicationInfo(
    page: Page,
    {copilotEnabled = true, hubEnabled = true}: AiApplicationInfoI = {}
): Promise<void> {
    await page.route('**/actuator/info', async (route: Route) => {
        const response = await route.fetch();

        const applicationInfo = await response.json();

        await route.fulfill({
            json: {
                ...applicationInfo,
                ai: {
                    ...applicationInfo.ai,
                    copilot: {...applicationInfo.ai?.copilot, enabled: String(copilotEnabled)},
                    hub: {...applicationInfo.ai?.hub, enabled: String(hubEnabled)},
                },
            },
            response,
        });
    });
}

/**
 * Answers `generateAiHubChatTitle` without a model: the request is re-sent as an `updateAiHubChat` mutation that sets
 * `chatTitle`, aliased so the client reads it as the title generation result. It waits for the chat patches already in
 * flight, since each one saves the whole chat and would otherwise put the old title back.
 */
export async function stubChatTitleGeneration(page: Page, chatTitle: string): Promise<void> {
    const pendingChatPatches = new Set<Request>();
    const patchesSettledWaiters: Array<() => void> = [];

    const settleChatPatch = (request: Request) => {
        if (!pendingChatPatches.delete(request) || pendingChatPatches.size > 0) {
            return;
        }

        patchesSettledWaiters.splice(0).forEach((resolveWaiter) => resolveWaiter());
    };

    page.on('request', (request) => {
        if (request.method() === 'POST' && readGraphQlOperationName(request) === 'updateAiHubChat') {
            pendingChatPatches.add(request);
        }
    });

    page.on('requestfinished', settleChatPatch);
    page.on('requestfailed', settleChatPatch);

    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST' || readGraphQlOperationName(request) !== 'generateAiHubChatTitle') {
            await route.fallback();

            return;
        }

        if (pendingChatPatches.size > 0) {
            await new Promise<void>((resolveWaiter) => patchesSettledWaiters.push(resolveWaiter));
        }

        const {variables} = request.postDataJSON() as {variables: {id: string; workspaceId: string}};

        await route.continue({
            postData: JSON.stringify({
                query: `mutation generateAiHubChatTitle($input: AiHubChatPatchInput!) {
                    generateAiHubChatTitle: updateAiHubChat(input: $input) {${AI_HUB_CHAT_FIELDS}}
                }`,
                variables: {input: {id: variables.id, title: chatTitle, workspaceId: variables.workspaceId}},
            }),
        });
    });
}

export interface PrepareAiHubOptionsI {
    chatTitle?: string;
    defaultModel?: {model: string; provider: string} | null;
    providerCatalog?: AiProviderCatalogType;
}

export async function prepareAiHub(
    page: Page,
    {chatTitle = `hub_chat_${getRandomString()}`, defaultModel, providerCatalog}: PrepareAiHubOptionsI = {}
): Promise<{chatTitle: string}> {
    await hideReactQueryDevtools(page);

    await overrideAiApplicationInfo(page);

    await stubAiProviders({defaultModel, page, providerCatalog: providerCatalog ?? ENABLED_PROVIDER_CATALOG});

    await stubChatTitleGeneration(page, chatTitle);

    return {chatTitle};
}

export async function readXsrfToken(page: Page): Promise<string> {
    const cookies = await page.context().cookies();

    return cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value ?? '';
}

export async function postGraphQl<DataType>(
    page: Page,
    query: string,
    variables: Record<string, unknown>
): Promise<DataType> {
    const response = await page.request.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    expect(response.ok(), `GraphQL answered ${response.status()}`).toBe(true);

    const responseBody = (await response.json()) as {data: DataType; errors?: Array<{message: string}>};

    expect(responseBody.errors, JSON.stringify(responseBody.errors)).toBeUndefined();

    return responseBody.data;
}

const readPersistedStoreValue = async (page: Page, storageKey: string, field: string) =>
    page.evaluate(
        ({fieldName, key}) => {
            const storedValue = window.localStorage.getItem(key);

            if (storedValue === null) {
                return undefined;
            }

            return (JSON.parse(storedValue) as {state?: Record<string, unknown>}).state?.[fieldName];
        },
        {fieldName: field, key: storageKey}
    );

/**
 * Resolves with the workspace id the app lists AI Hub chats for. Start it before the navigation that loads the hub.
 */
export async function waitForHubWorkspaceId(page: Page): Promise<number> {
    const chatsRequest = await page.waitForRequest(
        (request) => request.method() === 'POST' && readGraphQlOperationName(request) === 'aiHubChats'
    );

    return Number((chatsRequest.postDataJSON() as {variables: {workspaceId: string}}).variables.workspaceId);
}

export async function readAppEnvironmentId(page: Page): Promise<number> {
    const environmentId = await readPersistedStoreValue(page, 'bytechef.environment', 'currentEnvironmentId');

    return typeof environmentId === 'number' ? environmentId : 0;
}

export interface AiHubChatRefI {
    id: number;
    threadId: string;
    workspaceId: number;
}

export async function createAiHubChatThroughApi(
    page: Page,
    {environment, workspaceId}: {environment: number; workspaceId: number}
): Promise<AiHubChatRefI> {
    const data = await postGraphQl<{createAiHubChat: {id: string; threadId: string}}>(
        page,
        `mutation createAiHubChat($workspaceId: ID!, $environment: Int!, $threadId: String!) {
            createAiHubChat(workspaceId: $workspaceId, environment: $environment, threadId: $threadId) {id threadId}
        }`,
        {environment, threadId: getRandomString(), workspaceId: String(workspaceId)}
    );

    return {id: Number(data.createAiHubChat.id), threadId: data.createAiHubChat.threadId, workspaceId};
}

export async function updateAiHubChatThroughApi(
    page: Page,
    chat: AiHubChatRefI,
    patch: {status?: 'ACTIVE' | 'ARCHIVED'; title?: string}
): Promise<void> {
    await postGraphQl(
        page,
        `mutation updateAiHubChat($input: AiHubChatPatchInput!) { updateAiHubChat(input: $input) { id } }`,
        {input: {...patch, id: String(chat.id), workspaceId: String(chat.workspaceId)}}
    );
}

export async function deleteAiHubChatThroughApi(page: Page, chat: Omit<AiHubChatRefI, 'threadId'>): Promise<void> {
    const response = await page.request.post('/graphql', {
        data: {
            query: 'mutation deleteAiHubChat($workspaceId: ID!, $id: ID!) { deleteAiHubChat(workspaceId: $workspaceId, id: $id) }',
            variables: {id: String(chat.id), workspaceId: String(chat.workspaceId)},
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    if (!response.ok()) {
        console.warn(`Failed to delete AI Hub chat ${chat.id}: ${response.status()}`);
    }
}

/**
 * Records every chat the page creates through `createAiHubChat`, so a test can delete what it made.
 */
export function trackCreatedAiHubChats(page: Page): {chats: Array<Omit<AiHubChatRefI, 'threadId'>>} {
    const chats: Array<Omit<AiHubChatRefI, 'threadId'>> = [];

    page.on('response', async (response) => {
        const request = response.request();

        if (request.method() !== 'POST' || !response.url().endsWith('/graphql')) {
            return;
        }

        if (readGraphQlOperationName(request) !== 'createAiHubChat') {
            return;
        }

        try {
            const responseBody = (await response.json()) as {
                data?: {createAiHubChat?: {id: string; workspaceId: string}};
            };

            const createdChat = responseBody.data?.createAiHubChat;

            if (createdChat) {
                chats.push({id: Number(createdChat.id), workspaceId: Number(createdChat.workspaceId)});
            }
        } catch {
            return;
        }
    });

    return {chats};
}

const WORKFLOWS_API_PATH = '/api/automation/internal/workflows';

export interface HubWorkflowI {
    name: string;
    projectId: string;
    projectWorkflowId: number;
    workflowId: string;
}

export async function readHubWorkflow(
    page: Page,
    {projectId, projectWorkflowId}: {projectId: string; projectWorkflowId: number}
): Promise<HubWorkflowI> {
    const response = await page.request.get(`${WORKFLOWS_API_PATH}/by-project-workflow-id/${projectWorkflowId}`);

    expect(response.ok(), `reading project workflow ${projectWorkflowId} answered ${response.status()}`).toBe(true);

    const workflow = (await response.json()) as {id: string; label: string};

    return {name: workflow.label, projectId, projectWorkflowId, workflowId: workflow.id};
}

export async function deleteWorkflowThroughApi(page: Page, workflowId: string): Promise<void> {
    const response = await page.request.delete(`${WORKFLOWS_API_PATH}/${workflowId}`, {
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    expect(response.ok(), `deleting workflow ${workflowId} answered ${response.status()}`).toBe(true);
}

export const PROJECT_WORKFLOW_URL = (projectWorkflowId: number) =>
    new RegExp(`/api/automation/internal/workflows/by-project-workflow-id/${projectWorkflowId}(\\?.*)?$`);

export const openWorkflowTabResult = (workflow: HubWorkflowI) =>
    JSON.stringify({
        name: workflow.name,
        opened: true,
        projectId: workflow.projectId,
        projectWorkflowId: workflow.projectWorkflowId,
        workflowId: workflow.workflowId,
    });

export const openWorkflowTabArguments = (workflow: HubWorkflowI) => ({
    name: workflow.name,
    projectId: workflow.projectId,
    projectWorkflowId: workflow.projectWorkflowId,
    workflowId: workflow.workflowId,
});

export interface ToolApprovalRequestI {
    approvalId: number;
    arguments: Record<string, unknown>;
    expiresAt?: string | null;
    toolName: string;
}

export const toolApprovalRequestResult = ({
    approvalId,
    arguments: toolArguments,
    expiresAt = null,
    toolName,
}: ToolApprovalRequestI) =>
    JSON.stringify({
        approvalId,
        arguments: toolArguments,
        awaitingApproval: true,
        componentName: null,
        expiresAt,
        kind: 'tool-approval-request',
        toolName,
    });

/**
 * Serves the continuation turn the hub attaches to after an approval is decided. Each GET of the attach endpoint gets
 * the next scripted turn as one event stream.
 */
export async function stubAiHubAttach(page: Page, turns: AgUiTurnType[]): Promise<{attachRequests: string[]}> {
    const attachRequests: string[] = [];

    await page.route(AI_HUB_ATTACH_URL, async (route: Route) => {
        const threadId = new URL(route.request().url()).pathname.split('/').at(-2) ?? '';

        attachRequests.push(threadId);

        const turn = turns[attachRequests.length - 1];

        if (!turn) {
            await route.fulfill({json: {error: 'No in-flight run for thread'}, status: 404});

            return;
        }

        await route.fulfill({
            body: toSseBody(turn, {runId: getRandomString(), threadId}),
            contentType: 'text/event-stream',
            headers: {'Cache-Control': 'no-cache'},
            status: 200,
        });
    });

    return {attachRequests};
}

interface GraphQlErrorResponseI {
    errorCode: string;
    message: string;
}

/** Answers one GraphQL operation with an error, the way the hub's exception resolver reports it. */
export async function stubGraphQlOperationError(
    page: Page,
    operationName: string,
    {errorCode, message}: GraphQlErrorResponseI
): Promise<{calls: Array<Record<string, unknown>>}> {
    const calls: Array<Record<string, unknown>> = [];

    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST' || readGraphQlOperationName(request) !== operationName) {
            await route.fallback();

            return;
        }

        calls.push((request.postDataJSON() as {variables?: Record<string, unknown>}).variables ?? {});

        await route.fulfill({
            json: {
                data: null,
                errors: [{extensions: {classification: errorCode, errorCode}, message, path: [operationName]}],
            },
        });
    });

    return {calls};
}

export interface RecordedToastI {
    text: string;
    type: string;
}

/**
 * Records every toast the page shows, so a spec can assert on toasts that already closed themselves.
 */
export async function recordToasts(page: Page): Promise<void> {
    await page.addInitScript(() => {
        const recordedToasts: Array<{text: string; type: string}> = [];
        const seenToasts = new WeakSet<Element>();

        Object.assign(window, {__playwrightToasts: recordedToasts});

        const recordNewToasts = () => {
            document.querySelectorAll('[data-sonner-toast]').forEach((toastElement) => {
                if (seenToasts.has(toastElement)) {
                    return;
                }

                seenToasts.add(toastElement);

                recordedToasts.push({
                    text: toastElement.textContent ?? '',
                    type: toastElement.getAttribute('data-type') ?? '',
                });
            });
        };

        new MutationObserver(recordNewToasts).observe(document, {childList: true, subtree: true});
    });
}

export async function readRecordedToasts(page: Page): Promise<RecordedToastI[]> {
    return page.evaluate(() => (window as unknown as {__playwrightToasts?: RecordedToastI[]}).__playwrightToasts ?? []);
}

const CONNECTIONS_API_PATH = '/api/automation/internal/connections';

const PROJECTS_API_PATH = '/api/automation/internal/projects';

const PROJECT_DEPLOYMENTS_API_PATH = '/api/automation/internal/project-deployments';

const expectOk = async (
    response: {ok: () => boolean; status: () => number; text: () => Promise<string>},
    action: string
) => expect(response.ok(), `${action} answered ${response.status()}: ${await response.text()}`).toBe(true);

export async function createSlackConnectionThroughApi(
    page: Page,
    {environmentId, name, workspaceId}: {environmentId: number; name: string; workspaceId: number}
): Promise<number> {
    const response = await page.request.post(CONNECTIONS_API_PATH, {
        data: {
            authorizationType: 'OAUTH2_AUTHORIZATION_CODE',
            componentName: 'slack',
            connectionVersion: 1,
            environmentId,
            name,
            parameters: {access_token: `xoxb-playwright-${getRandomString()}`},
            workspaceId,
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `creating connection ${name}`);

    return Number(await response.text());
}

export async function createAnthropicConnectionThroughApi(
    page: Page,
    {
        apiKey,
        environmentId,
        name,
        workspaceId,
    }: {apiKey: string; environmentId: number; name: string; workspaceId: number}
): Promise<number> {
    const response = await page.request.post(CONNECTIONS_API_PATH, {
        data: {
            authorizationType: 'BEARER_TOKEN',
            componentName: 'anthropic',
            connectionVersion: 1,
            environmentId,
            name,
            parameters: {token: apiKey},
            workspaceId,
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `creating connection ${name}`);

    return Number(await response.text());
}

export async function deleteConnectionThroughApi(page: Page, connectionId: number): Promise<void> {
    const response = await page.request.delete(`${CONNECTIONS_API_PATH}/${connectionId}`, {
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    if (!response.ok() && response.status() !== 404) {
        console.warn(`Failed to delete connection ${connectionId}: ${response.status()}`);
    }
}

export interface ProjectSummaryI {
    id: number;
    lastProjectVersion?: number;
    lastPublishedDate?: string | number | null;
    lastStatus?: string;
    name: string;
}

export async function listWorkspaceProjects(page: Page, workspaceId: number): Promise<ProjectSummaryI[]> {
    const response = await page.request.get(`/api/automation/internal/workspaces/${workspaceId}/projects`);

    await expectOk(response, 'listing projects');

    return (await response.json()) as ProjectSummaryI[];
}

export async function findProjectByName(
    page: Page,
    workspaceId: number,
    projectName: string
): Promise<ProjectSummaryI | undefined> {
    return (await listWorkspaceProjects(page, workspaceId)).find((project) => project.name === projectName);
}

export async function readProject(page: Page, projectId: number): Promise<ProjectSummaryI | undefined> {
    const response = await page.request.get(`${PROJECTS_API_PATH}/${projectId}`);

    return response.ok() ? ((await response.json()) as ProjectSummaryI) : undefined;
}

export async function publishProjectThroughApi(page: Page, projectId: number, description: string): Promise<void> {
    const response = await page.request.post(`${PROJECTS_API_PATH}/${projectId}/publish`, {
        data: {description},
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `publishing project ${projectId}`);
}

export async function deleteProjectThroughApi(page: Page, projectId: number): Promise<void> {
    const response = await page.request.delete(`${PROJECTS_API_PATH}/${projectId}`, {
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    if (!response.ok() && response.status() !== 404) {
        console.warn(`Failed to delete project ${projectId}: ${response.status()}`);
    }
}

export interface ProjectDeploymentSummaryI {
    enabled: boolean;
    environmentId?: number;
    id: number;
    projectId: number;
    projectVersion: number;
}

export async function listProjectDeployments(
    page: Page,
    {projectId, workspaceId}: {projectId: number; workspaceId: number}
): Promise<ProjectDeploymentSummaryI[]> {
    const deployments: ProjectDeploymentSummaryI[] = [];

    for (const environmentId of [0, 1, 2]) {
        const response = await page.request.get(
            `/api/automation/internal/workspaces/${workspaceId}/project-deployments?projectId=${projectId}&environmentId=${environmentId}`
        );

        if (response.ok()) {
            deployments.push(...((await response.json()) as ProjectDeploymentSummaryI[]));
        }
    }

    return deployments;
}

export interface ProjectDeploymentConnectionI {
    connectionId: number;
    workflowConnectionKey: string;
    workflowNodeName: string;
}

/**
 * Deploys one workflow of a published project version and enables both the deployment and its workflow.
 */
export async function createEnabledProjectDeploymentThroughApi(
    page: Page,
    {
        connections,
        environmentId,
        name,
        projectId,
        projectVersion,
        workflowId,
    }: {
        connections: ProjectDeploymentConnectionI[];
        environmentId: number;
        name: string;
        projectId: number;
        projectVersion: number;
        workflowId: string;
    }
): Promise<number> {
    const response = await page.request.post(PROJECT_DEPLOYMENTS_API_PATH, {
        data: {
            enabled: true,
            environmentId,
            name,
            projectDeploymentWorkflows: [{connections, enabled: true, inputs: {}, workflowId}],
            projectId,
            projectVersion,
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `creating deployment ${name}`);

    const projectDeploymentId = Number(await response.text());

    const enableResponse = await page.request.patch(
        `${PROJECT_DEPLOYMENTS_API_PATH}/${projectDeploymentId}/enable/true`,
        {headers: {'X-XSRF-TOKEN': await readXsrfToken(page)}}
    );

    await expectOk(enableResponse, `enabling deployment ${projectDeploymentId}`);

    return projectDeploymentId;
}

export async function deleteProjectDeploymentThroughApi(page: Page, projectDeploymentId: number): Promise<void> {
    const disableResponse = await page.request.patch(
        `${PROJECT_DEPLOYMENTS_API_PATH}/${projectDeploymentId}/enable/false`,
        {headers: {'X-XSRF-TOKEN': await readXsrfToken(page)}}
    );

    if (!disableResponse.ok() && disableResponse.status() !== 404) {
        console.warn(`Failed to disable project deployment ${projectDeploymentId}: ${disableResponse.status()}`);
    }

    const deleteResponse = await page.request.delete(`${PROJECT_DEPLOYMENTS_API_PATH}/${projectDeploymentId}`, {
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    if (!deleteResponse.ok() && deleteResponse.status() !== 404) {
        console.warn(`Failed to delete project deployment ${projectDeploymentId}: ${deleteResponse.status()}`);
    }
}

export interface WorkflowComponentTypesI {
    taskTypes: string[];
    triggerTypes: string[];
}

export async function readWorkflowComponentTypes(page: Page, workflowId: string): Promise<WorkflowComponentTypesI> {
    const response = await page.request.get(`${WORKFLOWS_API_PATH}/${workflowId}`);

    await expectOk(response, `reading workflow ${workflowId}`);

    const workflow = (await response.json()) as {definition?: string};

    const definition = JSON.parse(workflow.definition ?? '{}') as {
        tasks?: Array<{type: string}>;
        triggers?: Array<{type: string}>;
    };

    return {
        taskTypes: (definition.tasks ?? []).map((task) => task.type).sort(),
        triggerTypes: (definition.triggers ?? []).map((trigger) => trigger.type).sort(),
    };
}

export interface RecordedToolEventI {
    arguments?: string;
    id?: string;
    kind: string;
    name?: string;
    response?: string;
}

/** The tool calls and results the server kept for a chat, in transcript order. */
export async function readChatToolEvents(
    page: Page,
    chat: Omit<AiHubChatRefI, 'threadId'>
): Promise<RecordedToolEventI[]> {
    const data = await postGraphQl<{aiHubChatMessages: Array<{toolEventsJson: string | null}>}>(
        page,
        'query aiHubChatMessages($workspaceId: ID!, $id: ID!) { aiHubChatMessages(workspaceId: $workspaceId, id: $id) { toolEventsJson } }',
        {id: String(chat.id), workspaceId: String(chat.workspaceId)}
    );

    return data.aiHubChatMessages.flatMap((message) =>
        message.toolEventsJson ? (JSON.parse(message.toolEventsJson) as RecordedToolEventI[]) : []
    );
}

export const calledToolNames = (toolEvents: RecordedToolEventI[]) =>
    toolEvents.filter((toolEvent) => toolEvent.kind === 'call').map((toolEvent) => toolEvent.name ?? '');

export interface ToolApprovalSummaryI {
    id: string;
    status: string;
    toolName: string;
}

export async function readToolApprovals(
    page: Page,
    chat: Omit<AiHubChatRefI, 'threadId'>
): Promise<ToolApprovalSummaryI[]> {
    const data = await postGraphQl<{aiHubToolApprovals: ToolApprovalSummaryI[]}>(
        page,
        'query aiHubToolApprovals($workspaceId: ID!, $chatId: ID!) { aiHubToolApprovals(workspaceId: $workspaceId, chatId: $chatId) { id status toolName } }',
        {chatId: String(chat.id), workspaceId: String(chat.workspaceId)}
    );

    return data.aiHubToolApprovals;
}

export async function readDefaultGatedToolNames(page: Page): Promise<string[]> {
    const data = await postGraphQl<{aiHubToolApprovalDefaultToolNames: string[]}>(
        page,
        'query aiHubToolApprovalDefaultToolNames { aiHubToolApprovalDefaultToolNames }',
        {}
    );

    return data.aiHubToolApprovalDefaultToolNames;
}

export interface ToolApprovalRuleI {
    id: string;
    mode: string;
    toolKind: string;
    toolName: string;
}

export async function readToolApprovalRules(page: Page, workspaceId: number): Promise<ToolApprovalRuleI[]> {
    const data = await postGraphQl<{aiHubToolApprovalRules: ToolApprovalRuleI[]}>(
        page,
        'query aiHubToolApprovalRules($workspaceId: ID!) { aiHubToolApprovalRules(workspaceId: $workspaceId) { id toolKind toolName mode } }',
        {workspaceId: String(workspaceId)}
    );

    return data.aiHubToolApprovalRules;
}

export async function createToolApprovalRuleThroughApi(
    page: Page,
    {mode, toolName, workspaceId}: {mode: 'EXEMPT' | 'REQUIRE'; toolName: string; workspaceId: number}
): Promise<string> {
    const data = await postGraphQl<{createAiHubToolApprovalRule: {id: string}}>(
        page,
        `mutation createAiHubToolApprovalRule($workspaceId: ID!, $toolKind: AiHubToolKind!, $toolName: String!, $mode: AiHubToolApprovalRuleMode!) {
            createAiHubToolApprovalRule(workspaceId: $workspaceId, toolKind: $toolKind, toolName: $toolName, mode: $mode) { id }
        }`,
        {mode, toolKind: 'CATALOG', toolName, workspaceId: String(workspaceId)}
    );

    return data.createAiHubToolApprovalRule.id;
}

export async function deleteToolApprovalRuleThroughApi(
    page: Page,
    {ruleId, workspaceId}: {ruleId: string; workspaceId: number}
): Promise<void> {
    await postGraphQl(
        page,
        'mutation deleteAiHubToolApprovalRule($workspaceId: ID!, $ruleId: ID!) { deleteAiHubToolApprovalRule(workspaceId: $workspaceId, ruleId: $ruleId) }',
        {ruleId, workspaceId: String(workspaceId)}
    );
}

export async function createProjectThroughApi(
    page: Page,
    {name, workspaceId}: {name: string; workspaceId: number}
): Promise<number> {
    const response = await page.request.post(PROJECTS_API_PATH, {
        data: {name, workspaceId},
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `creating project ${name}`);

    return Number(await response.text());
}

export async function createProjectWorkflowThroughApi(
    page: Page,
    {definition, projectId}: {definition: object; projectId: number}
): Promise<void> {
    const response = await page.request.post(`${PROJECTS_API_PATH}/${projectId}/workflows`, {
        data: {definition: JSON.stringify(definition)},
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `adding a workflow to project ${projectId}`);
}

export async function listProjectWorkflowIds(page: Page, projectId: number): Promise<string[]> {
    const response = await page.request.get(`${PROJECTS_API_PATH}/${projectId}/workflows`);

    await expectOk(response, `listing the workflows of project ${projectId}`);

    return ((await response.json()) as Array<{id: string}>).map((workflow) => workflow.id);
}

export interface WorkflowNodeI {
    name: string;
    parameters: Record<string, unknown>;
    type: string;
}

const isWorkflowNode = (value: unknown): value is WorkflowNodeI =>
    typeof value === 'object' &&
    value !== null &&
    typeof (value as {type?: unknown}).type === 'string' &&
    /^[A-Za-z0-9]+\/v\d+/.test((value as {type: string}).type);

function collectWorkflowNodes(value: unknown, workflowNodes: WorkflowNodeI[]): void {
    if (Array.isArray(value)) {
        value.forEach((item) => collectWorkflowNodes(item, workflowNodes));

        return;
    }

    if (typeof value !== 'object' || value === null) {
        return;
    }

    if (isWorkflowNode(value)) {
        workflowNodes.push({
            name: String((value as {name?: unknown}).name ?? ''),
            parameters: ((value as {parameters?: unknown}).parameters ?? {}) as Record<string, unknown>,
            type: value.type,
        });
    }

    Object.values(value).forEach((child) => collectWorkflowNodes(child, workflowNodes));
}

/**
 * Reads every node of a saved workflow: triggers, tasks, the tasks nested in task dispatchers, and every cluster
 * element, at any depth.
 */
export async function readWorkflowNodes(page: Page, workflowId: string): Promise<WorkflowNodeI[]> {
    const response = await page.request.get(`${WORKFLOWS_API_PATH}/${workflowId}`);

    await expectOk(response, `reading workflow ${workflowId}`);

    const workflow = (await response.json()) as {definition?: string};
    const definition = JSON.parse(workflow.definition ?? '{}') as {tasks?: unknown[]; triggers?: unknown[]};
    const workflowNodes: WorkflowNodeI[] = [];

    collectWorkflowNodes([...(definition.triggers ?? []), ...(definition.tasks ?? [])], workflowNodes);

    return workflowNodes;
}

export async function createDataTableThroughApi(
    page: Page,
    {
        baseName,
        columns,
        environmentId,
        workspaceId,
    }: {baseName: string; columns: Array<{name: string; type: string}>; environmentId: number; workspaceId: number}
): Promise<string> {
    await postGraphQl(
        page,
        'mutation createDataTable($input: CreateDataTableInput!) { createDataTable(input: $input) }',
        {
            input: {baseName, columns, environmentId: String(environmentId), workspaceId: String(workspaceId)},
        }
    );

    const data = await postGraphQl<{dataTables: Array<{baseName: string; id: string}>}>(
        page,
        'query dataTables($environmentId: ID!, $workspaceId: ID!) { dataTables(environmentId: $environmentId, workspaceId: $workspaceId) { id baseName } }',
        {environmentId: String(environmentId), workspaceId: String(workspaceId)}
    );

    const dataTable = data.dataTables.find((candidate) => candidate.baseName === baseName);

    expect(dataTable, `data table ${baseName} was not created`).toBeDefined();

    return dataTable!.id;
}

export async function dropDataTableThroughApi(
    page: Page,
    {environmentId, tableId}: {environmentId: number; tableId: string}
): Promise<void> {
    const response = await page.request.post('/graphql', {
        data: {
            query: 'mutation dropDataTable($input: RemoveTableInput!) { dropDataTable(input: $input) }',
            variables: {input: {environmentId: String(environmentId), tableId}},
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    if (!response.ok()) {
        console.warn(`Failed to drop data table ${tableId}: ${response.status()}`);
    }
}

export async function listConnections(
    page: Page,
    {componentName, environmentId, workspaceId}: {componentName: string; environmentId: number; workspaceId: number}
): Promise<Array<{id: number; name: string}>> {
    const response = await page.request.get(
        `/api/automation/internal/workspaces/${workspaceId}/connections?componentName=${componentName}&environmentId=${environmentId}`
    );

    await expectOk(response, `listing ${componentName} connections`);

    return (await response.json()) as Array<{id: number; name: string}>;
}

const CARD_CHROME_BUTTON_NAMES = new Set([
    'Copy',
    'Dismiss',
    'More',
    'Next',
    'Other…',
    'Refresh',
    'Retry',
    'Submit all',
]);

const SUBMITTING_BUTTON_NAME = 'Submitting…';

const MAX_CARD_ROUNDS = 5;

const CONNECTIONS_LOADING_TEXT = /^Loading .+ connections\.\.\.$/;

export interface InteractiveCardAnswerI {
    answer: string;
    cardText: string;
}

/**
 * Answers the interactive cards a turn ends with, one round at a time, until the last assistant message offers nothing
 * more to answer: a question card's option buttons or checkboxes, or a picker card's combobox (its first option).
 * Returns each card's text with the answer given.
 */
export async function answerInteractiveCards(
    page: Page,
    {
        answerPickers = true,
        chooseOption = (cardText, optionNames) => optionNames[0],
        readLastMessage,
        waitForTurnEnd,
    }: {
        answerPickers?: boolean;
        chooseOption?: (cardText: string, optionNames: string[]) => string;
        readLastMessage: () => Locator;
        waitForTurnEnd: () => Promise<void>;
    }
): Promise<InteractiveCardAnswerI[]> {
    const answers: InteractiveCardAnswerI[] = [];

    for (let round = 0; round < MAX_CARD_ROUNDS; round++) {
        const lastMessage = readLastMessage();

        await expect(lastMessage.getByText(CONNECTIONS_LOADING_TEXT)).toHaveCount(0);

        await expect(lastMessage.getByRole('button', {name: SUBMITTING_BUTTON_NAME})).toHaveCount(0, {
            timeout: 60000,
        });

        const cardText = await lastMessage.innerText();
        const picker = lastMessage.getByRole('combobox');

        if ((await picker.count()) > 0 && (await picker.first().isEnabled())) {
            if (!answerPickers) {
                break;
            }

            await picker.first().click();

            const firstOption = page.getByRole('option').first();
            const optionName = (await firstOption.innerText()).trim();

            await firstOption.click();

            answers.push({answer: optionName, cardText});

            await waitForTurnEnd();

            continue;
        }

        const checkboxLabels = lastMessage.locator('label').filter({has: page.getByRole('checkbox')});

        if ((await checkboxLabels.count()) > 0) {
            const checkboxNames = (await checkboxLabels.allInnerTexts()).map(
                (checkboxName) => checkboxName.trim().split('\n')[0]
            );

            const answer = chooseOption(cardText, checkboxNames);

            answers.push({answer, cardText});

            await checkboxLabels.nth(checkboxNames.indexOf(answer)).getByRole('checkbox').check();

            await lastMessage.getByRole('button', {name: /^(Next|Submit all)$/}).click();

            await waitForTurnEnd();

            continue;
        }

        const optionNames = (await lastMessage.getByRole('button').allInnerTexts())
            .map((optionName) => optionName.trim())
            .filter(
                (optionName) =>
                    optionName && !CARD_CHROME_BUTTON_NAMES.has(optionName) && !optionName.startsWith('Connect ')
            );

        if (optionNames.length === 0) {
            break;
        }

        const answer = chooseOption(cardText, optionNames);

        answers.push({answer, cardText});

        await lastMessage.getByRole('button', {exact: true, name: answer}).click();

        await waitForTurnEnd();
    }

    return answers;
}

export async function createBearerTokenConnectionThroughApi(
    page: Page,
    {
        componentName,
        environmentId,
        name,
        token,
        workspaceId,
    }: {componentName: string; environmentId: number; name: string; token: string; workspaceId: number}
): Promise<number> {
    const response = await page.request.post(CONNECTIONS_API_PATH, {
        data: {
            authorizationType: 'BEARER_TOKEN',
            componentName,
            connectionVersion: 1,
            environmentId,
            name,
            parameters: {token},
            workspaceId,
        },
        headers: {'X-XSRF-TOKEN': await readXsrfToken(page)},
    });

    await expectOk(response, `creating connection ${name}`);

    return Number(await response.text());
}
