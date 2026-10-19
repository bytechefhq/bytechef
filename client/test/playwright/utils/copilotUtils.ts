import {type Page, type Request, type Route} from '@playwright/test';

import {CopilotPanel} from '../pages/copilotPanel';
import {WorkflowPage} from '../pages/workflowPage';
import {type TestWorkflowI} from './projectUtils';

import type {AiProviderCatalogQuery} from '../../../src/shared/middleware/graphql';

export const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

export const OPENAI_PROVIDER_KEY = 'ai.provider.openAi';

export const DEFAULT_MODEL = {label: 'GPT-4o', name: 'gpt-4o'} as const;

export const ALTERNATE_MODEL = {label: 'GPT-4.1 mini', name: 'gpt-4.1-mini'} as const;

export type AiProviderCatalogType = AiProviderCatalogQuery['aiProviderCatalog'];

export const ENABLED_PROVIDER_CATALOG: AiProviderCatalogType = [
    {
        enabled: true,
        icon: null,
        key: OPENAI_PROVIDER_KEY,
        models: [
            {label: DEFAULT_MODEL.label, name: DEFAULT_MODEL.name},
            {label: ALTERNATE_MODEL.label, name: ALTERNATE_MODEL.name},
        ],
        name: 'OpenAI',
        supportsModelById: false,
    },
];

export const DISABLED_PROVIDER_CATALOG: AiProviderCatalogType = [{...ENABLED_PROVIDER_CATALOG[0], enabled: false}];

interface GraphQlRequestBodyI {
    query?: string;
    variables?: Record<string, unknown>;
}

const GRAPHQL_OPERATION_NAME_PATTERN = /^\s*(?:query|mutation)\s+(\w+)/;

export function readGraphQlOperationName(request: Request): string | undefined {
    const requestBody = request.postDataJSON() as GraphQlRequestBodyI | null;

    return requestBody?.query?.match(GRAPHQL_OPERATION_NAME_PATTERN)?.[1];
}

export async function stubGraphQlOperation(
    page: Page,
    operationName: string,
    resolveData: (variables: Record<string, unknown>) => unknown
): Promise<void> {
    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST' || readGraphQlOperationName(request) !== operationName) {
            await route.fallback();

            return;
        }

        const requestBody = request.postDataJSON() as GraphQlRequestBodyI;

        await route.fulfill({json: {data: resolveData(requestBody.variables ?? {})}});
    });
}

export async function enableCopilotInApplicationInfo(page: Page): Promise<void> {
    await page.route('**/actuator/info', async (route: Route) => {
        const response = await route.fetch();

        const applicationInfo = await response.json();

        await route.fulfill({
            json: {
                ...applicationInfo,
                ai: {...applicationInfo.ai, copilot: {...applicationInfo.ai?.copilot, enabled: 'true'}},
            },
            response,
        });
    });
}

interface StubAiProvidersPropsI {
    defaultModel?: {model: string; provider: string} | null;
    page: Page;
    providerCatalog?: AiProviderCatalogType;
}

export async function stubAiProviders({
    defaultModel = {model: DEFAULT_MODEL.name, provider: OPENAI_PROVIDER_KEY},
    page,
    providerCatalog = ENABLED_PROVIDER_CATALOG,
}: StubAiProvidersPropsI): Promise<void> {
    await stubGraphQlOperation(page, 'aiDefaultModel', () => ({aiDefaultModel: defaultModel}));

    await stubGraphQlOperation(page, 'aiProviderCatalog', () => ({aiProviderCatalog: providerCatalog}));
}

export async function hideReactQueryDevtools(page: Page): Promise<void> {
    await page.addInitScript(() => {
        document.addEventListener('DOMContentLoaded', () => {
            const devtoolsHidingStyle = document.createElement('style');

            devtoolsHidingStyle.textContent = '.tsqd-parent-container { display: none !important; }';

            document.head.appendChild(devtoolsHidingStyle);
        });
    });
}

export async function prepareCopilot(page: Page, stubAiProvidersProps?: Omit<StubAiProvidersPropsI, 'page'>) {
    await hideReactQueryDevtools(page);

    await enableCopilotInApplicationInfo(page);

    await stubAiProviders({...stubAiProvidersProps, page});
}

export async function navigateInApp(page: Page, path: string): Promise<void> {
    await page.evaluate((targetPath) => {
        window.history.pushState(null, '', targetPath);

        window.dispatchEvent(new PopStateEvent('popstate', {state: null}));
    }, path);

    await page.waitForURL((url) => url.pathname === path);
}

const DEFAULT_WORKSPACE_ID = 1049;

export async function readCurrentWorkspaceId(page: Page): Promise<number> {
    return page.evaluate((defaultWorkspaceId) => {
        const persistedWorkspaceStore = window.localStorage.getItem('bytechef.workspace');

        const currentWorkspaceId = persistedWorkspaceStore
            ? JSON.parse(persistedWorkspaceStore)?.state?.currentWorkspaceId
            : undefined;

        return typeof currentWorkspaceId === 'number' ? currentWorkspaceId : defaultWorkspaceId;
    }, DEFAULT_WORKSPACE_ID);
}

export const lastUsedModelStorageKey = (workspaceId: number) => `bytechef.modelPicker.lastUsed.${workspaceId}`;

export async function openWorkflowEditorCopilot(page: Page, workflow: TestWorkflowI): Promise<CopilotPanel> {
    const workflowPage = new WorkflowPage(page);

    await workflowPage.goToWorkflowEditor(workflow.projectId, workflow.workflowId);

    const copilotPanel = new CopilotPanel(page);

    await copilotPanel.openFromWorkflowEditor();

    return copilotPanel;
}

export interface StubbedConnectionI {
    id: number;
    name: string;
}

export const WORKSPACE_CONNECTIONS_URL = /\/api\/automation\/internal\/workspaces\/\d+\/connections(\?.*)?$/;

export async function stubWorkspaceConnections(page: Page, connections: StubbedConnectionI[]): Promise<void> {
    await page.route(WORKSPACE_CONNECTIONS_URL, async (route: Route) => {
        if (route.request().method() !== 'GET') {
            await route.fallback();

            return;
        }

        await route.fulfill({
            json: connections.map((connection) => ({
                active: true,
                componentName: 'slack',
                connectionVersion: 1,
                id: connection.id,
                name: connection.name,
                tags: [],
            })),
        });
    });
}
