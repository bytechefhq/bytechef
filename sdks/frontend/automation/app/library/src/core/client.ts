import {
    assertServerRuntime,
    type BytechefClientOptions,
    DEFAULT_CONTEXT_TTL_MS,
    resolveConfig,
    type ResolvedConfig,
} from './config';
import {type ApiApi, createApiApi} from './api';
import {type AuthApiI, createAuthApi} from './auth';
import {createActionsApi, createConnectionsApi} from './connections';
import {type ContextApiI, createContextApi} from './context';
import {createProxyApi} from './proxy';
import {createRequester} from './request';
import {createToolsApi} from './tools';
import {createWorkflowsApi, type WorkflowsApi} from './workflows';

export interface BytechefClient {
    api: ApiApi;
    actions: ReturnType<typeof createActionsApi>;
    auth: AuthApiI;
    connections: ReturnType<typeof createConnectionsApi>;
    context: ContextApiI;
    proxy: ReturnType<typeof createProxyApi>;
    tools: ReturnType<typeof createToolsApi>;
    workflows: WorkflowsApi;
}

export function createBytechefClient(options: BytechefClientOptions = {}): BytechefClient {
    assertServerRuntime();

    let resolved: ResolvedConfig | undefined;

    const getConfig = () => {
        resolved ??= resolveConfig(options);

        return resolved;
    };
    const request = createRequester(getConfig);
    const context = createContextApi(request, () => options.contextTtlMs ?? DEFAULT_CONTEXT_TTL_MS);

    return {
        actions: createActionsApi(request),
        api: createApiApi(request),
        auth: createAuthApi(request, getConfig, context),
        connections: createConnectionsApi(request),
        context,
        proxy: createProxyApi(request),
        tools: createToolsApi(request),
        workflows: createWorkflowsApi(request, () => getConfig().timeoutMs),
    };
}
