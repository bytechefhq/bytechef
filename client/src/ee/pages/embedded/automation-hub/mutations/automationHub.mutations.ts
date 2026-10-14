import {AutomationHubKeys} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {ConnectedUserProjectWorkflowApi, ConnectionApi} from '@bytechef/embedded-api';
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

const omitKeys = <T>(source: Record<string, T>, keysToOmit: string[]): Record<string, T> =>
    Object.fromEntries(Object.entries(source).filter(([key]) => !keysToOmit.includes(key)));

export interface ProvisionReferenceRequestI {
    connections: Record<string, number>;
    inputs?: Record<string, unknown>;
    providedComponentNames?: string[];
    providedInputNames?: string[];
    workflowUuid: string;
}

export const useProvisionReferenceMutation = () => {
    const queryClient = useQueryClient();

    return useMutation<void, Error, ProvisionReferenceRequestI>({
        mutationFn: ({connections, inputs, providedComponentNames = [], providedInputNames = [], workflowUuid}) =>
            new ConnectedUserProjectWorkflowApi().provisionFrontendWorkflowReference({
                provisionWorkflowReferenceRequest: {
                    connections: omitKeys(connections, providedComponentNames),
                    inputs: inputs && omitKeys(inputs, providedInputNames),
                },
                workflowUuid,
            }),
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
        mutationFn: async ({enabled, workflowUuid}) => {
            if (enabled) {
                await new ConnectedUserProjectWorkflowApi().enableFrontendProjectWorkflow({workflowUuid});

                return;
            }

            await new ConnectedUserProjectWorkflowApi().disableFrontendProjectWorkflow({workflowUuid});
        },
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
