import {
    AutomationWorkflowProject,
    AutomationWorkflowProjectApi,
    ConnectedUserProjectWorkflow,
    ConnectedUserProjectWorkflowApi,
    Connection,
    ConnectionApi,
} from '@/ee/shared/middleware/embedded/public';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';

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
        select: (projects) => projects.filter((project) => project.automationHubVisible !== false),
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

export const useGetComponentConnectionsQuery = (componentName: string, enabled = true) =>
    useQuery<Connection[]>({
        enabled,
        queryFn: () => new ConnectionApi().getFrontendConnections({componentName}),
        queryKey: AutomationHubKeys.connectionsByComponent(componentName),
    });

export const useGetWorkflowQuery = (workflowUuid?: string) =>
    useQuery<ConnectedUserProjectWorkflow>({
        enabled: !!workflowUuid,
        queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflow({workflowUuid: workflowUuid!}),
        queryKey: AutomationHubKeys.workflow(workflowUuid!),
    });

export const useFetchWorkflow = () => {
    const queryClient = useQueryClient();

    return useCallback(
        (workflowUuid: string) =>
            queryClient.query<ConnectedUserProjectWorkflow>({
                queryFn: () => new ConnectedUserProjectWorkflowApi().getFrontendProjectWorkflow({workflowUuid}),
                queryKey: AutomationHubKeys.workflow(workflowUuid),
            }),
        [queryClient]
    );
};
