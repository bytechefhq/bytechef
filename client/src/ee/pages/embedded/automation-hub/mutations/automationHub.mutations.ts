import {AutomationHubKeys} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {ConnectedUserProjectWorkflowApi, ConnectionApi} from '@/ee/shared/middleware/embedded/public';
import {useMutation, useQueryClient} from '@tanstack/react-query';

export const BLANK_DEFINITION = JSON.stringify({description: '', label: 'New automation', tasks: [], triggers: []});

export const useCopyTemplateMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<string, Error, string>({
        mutationFn: (workflowUuid) =>
            new ConnectedUserProjectWorkflowApi().copyFrontendWorkflowTemplate({workflowUuid}),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export const useDeprovisionReferenceMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, string>({
        mutationFn: (workflowUuid) =>
            new ConnectedUserProjectWorkflowApi().deprovisionFrontendWorkflowReference({workflowUuid}),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export interface SetAutomationEnabledRequestI {
    enabled: boolean;
    workflowUuid: string;
}

export const useSetAutomationEnabledMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, SetAutomationEnabledRequestI>({
        mutationFn: ({enabled, workflowUuid}) =>
            enabled
                ? new ConnectedUserProjectWorkflowApi().enableFrontendProjectWorkflow({workflowUuid})
                : new ConnectedUserProjectWorkflowApi().disableFrontendProjectWorkflow({workflowUuid}),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export const useDeleteAutomationMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, string>({
        mutationFn: (workflowUuid) =>
            new ConnectedUserProjectWorkflowApi().deleteFrontendProjectWorkflow({workflowUuid}),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export const usePublishAutomationMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, string>({
        mutationFn: (workflowUuid) =>
            new ConnectedUserProjectWorkflowApi().publishFrontendProjectWorkflow({
                publishFrontendProjectWorkflowRequest: {description: ''},
                workflowUuid,
            }),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export const useUpdateAutomationInputsMutation = () =>
    useMutation<void, Error, {inputs: Record<string, unknown>; workflowUuid: string}>({
        mutationFn: ({inputs, workflowUuid}) =>
            new ConnectedUserProjectWorkflowApi().updateFrontendProjectWorkflowInputs({
                updateWorkflowInputsRequest: {inputs},
                workflowUuid,
            }),
    });

export const useCreateBlankAutomationMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<string, Error, void>({
        mutationFn: () =>
            new ConnectedUserProjectWorkflowApi().createFrontendProjectWorkflow({
                createFrontendProjectWorkflowRequest: {definition: BLANK_DEFINITION},
            }),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.automations});
        },
    });
};

export interface WireNodeConnectionRequestI {
    connectionId: number;
    workflowConnectionKey: string;
    workflowNodeName: string;
    workflowUuid: string;
}

export const useWireNodeConnectionMutation = () =>
    useMutation<void, Error, WireNodeConnectionRequestI>({
        mutationFn: ({connectionId, workflowConnectionKey, workflowNodeName, workflowUuid}) =>
            new ConnectedUserProjectWorkflowApi().updateFrontendWorkflowConfigurationConnection({
                updateFrontendWorkflowConfigurationConnectionRequest: {connectionId},
                workflowConnectionKey,
                workflowNodeName,
                workflowUuid,
            }),
    });

export const useDeleteHubConnectionMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, number>({
        mutationFn: (id) => new ConnectionApi().deleteFrontendConnection({id}),
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: AutomationHubKeys.connections});
        },
    });
};
