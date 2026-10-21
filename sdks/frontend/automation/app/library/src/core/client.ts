import {assertServerRuntime, type BytechefClientOptions, resolveConfig, type ResolvedConfig} from './config';
import {type ApiApi, createApiApi} from './api';
import {createActionsApi, createConnectionsApi} from './connections';
import {createProxyApi} from './proxy';
import {createRequester} from './request';
import {createToolsApi} from './tools';
import {createWorkflowsApi, type WorkflowsApi} from './workflows';

export interface BytechefClient {
    api: ApiApi;
    actions: ReturnType<typeof createActionsApi>;
    connections: ReturnType<typeof createConnectionsApi>;
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

    return {
        actions: createActionsApi(request),
        api: createApiApi(request),
        connections: createConnectionsApi(request),
        proxy: createProxyApi(request),
        tools: createToolsApi(request),
        workflows: createWorkflowsApi(request, () => getConfig().timeoutMs),
    };
}
