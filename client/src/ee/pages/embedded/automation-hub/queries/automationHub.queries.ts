import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {
    AutomationWorkflowProject,
    AutomationWorkflowProjectApi,
    ConnectedUserProjectWorkflow,
    ConnectedUserProjectWorkflowApi,
    Connection,
    ConnectionApi,
} from '@/ee/shared/middleware/embedded/public';
import {useQuery} from '@tanstack/react-query';

export const AutomationHubKeys = {
    automations: ['automationHub', 'automations'] as const,
    connections: ['automationHub', 'connections'] as const,
    connectionsByComponent: (componentName: string) => ['automationHub', 'connections', componentName] as const,
    templates: ['automationHub', 'templates'] as const,
    workflow: (workflowUuid: string) => ['automationHub', 'workflow', workflowUuid] as const,
};

export const useGetTemplateProjectsQuery = () =>
    useQuery<AutomationWorkflowProject[]>({
        queryFn: () => new AutomationWorkflowProjectApi().getFrontendProjects({}),
        queryKey: AutomationHubKeys.templates,
    });

export const useGetAutomationsQuery = () =>
    useQuery<ConnectedUserProjectWorkflow[]>({
        queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflows({}),
        queryKey: AutomationHubKeys.automations,
    });

export const useGetConnectionsQuery = () =>
    useQuery<Connection[]>({
        queryFn: () => new ConnectionApi().getAllFrontendConnections({}),
        queryKey: AutomationHubKeys.connections,
    });

/**
 * The vendor's `sharedConnectionIds` are unioned onto the connected user's own connections
 * server-side and then filtered by component (`ConnectedUserConnectionFacadeImpl#getConnections`),
 * so they are the only way a vendor-shared connection ever reaches an account select — which is
 * what the SDK's `sharedConnectionIds` prop documents. They are read from the store here rather
 * than threaded through every caller so the wizard's account select and its "Connected accounts"
 * summary agree on what exists.
 *
 * Deliberately NOT part of the query key: the ids arrive once with the EMBED_INIT handshake, which
 * `AutomationHubLayout` completes before any view renders, and never change afterwards. Keeping the
 * key at three elements also keeps `connections` a plain prefix of it, which is what makes the
 * connection dialog's `connectionsQueryKey` invalidation cascade to the per-component queries.
 */
export const useGetComponentConnectionsQuery = (componentName: string, enabled = true) => {
    const sharedConnectionIds = useAutomationHubStore((state) => state.sharedConnectionIds);

    return useQuery<Connection[]>({
        enabled,
        queryFn: () =>
            new ConnectionApi().getFrontendConnections({
                componentName,
                // The generated client emits any non-null value as a query parameter, so an empty
                // array would put a meaningless `connectionIds=` on every request.
                connectionIds: sharedConnectionIds.length > 0 ? sharedConnectionIds : undefined,
            }),
        queryKey: AutomationHubKeys.connectionsByComponent(componentName),
    });
};

export const useGetWorkflowQuery = (workflowUuid?: string) =>
    useQuery<ConnectedUserProjectWorkflow>({
        enabled: !!workflowUuid,
        queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflow({workflowUuid: workflowUuid!}),
        queryKey: AutomationHubKeys.workflow(workflowUuid!),
    });
