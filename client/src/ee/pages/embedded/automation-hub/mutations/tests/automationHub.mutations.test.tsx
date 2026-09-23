import {
    BLANK_DEFINITION,
    useCopyTemplateMutation,
    useCreateBlankAutomationMutation,
    useDeleteAutomationMutation,
    useDeleteHubConnectionMutation,
    useDeprovisionReferenceMutation,
    usePublishAutomationMutation,
    useSetAutomationEnabledMutation,
    useUpdateAutomationInputsMutation,
    useWireNodeConnectionMutation,
} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import {AutomationHubKeys} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {connectedUserProjectWorkflowApiMock, connectionApiMock} = vi.hoisted(() => ({
    connectedUserProjectWorkflowApiMock: {
        copyFrontendWorkflowTemplate: vi.fn(),
        createFrontendProjectWorkflow: vi.fn(),
        deleteFrontendProjectWorkflow: vi.fn(),
        deprovisionFrontendWorkflowReference: vi.fn(),
        disableFrontendProjectWorkflow: vi.fn(),
        enableFrontendProjectWorkflow: vi.fn(),
        publishFrontendProjectWorkflow: vi.fn(),
        updateFrontendProjectWorkflowInputs: vi.fn(),
        updateFrontendWorkflowConfigurationConnection: vi.fn(),
    },
    connectionApiMock: {
        deleteFrontendConnection: vi.fn(),
    },
}));

vi.mock('@/ee/shared/middleware/embedded/public', () => ({
    ConnectedUserProjectWorkflowApi: class {
        constructor() {
            return connectedUserProjectWorkflowApiMock;
        }
    },
    ConnectionApi: class {
        constructor() {
            return connectionApiMock;
        }
    },
}));

const renderMutationHook = <T,>(useHook: () => T) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}}});
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const {result} = renderHook(useHook, {wrapper});

    return {invalidateQueriesSpy, result};
};

describe('automationHub.mutations', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        Object.values(connectedUserProjectWorkflowApiMock).forEach((apiMethod) =>
            apiMethod.mockResolvedValue(undefined)
        );
        connectionApiMock.deleteFrontendConnection.mockResolvedValue(undefined);
    });

    it('copies a template and refreshes the automations', async () => {
        connectedUserProjectWorkflowApiMock.copyFrontendWorkflowTemplate.mockResolvedValue('copied-uuid');

        const {invalidateQueriesSpy, result} = renderMutationHook(useCopyTemplateMutation);

        let copiedWorkflowUuid: string | undefined;

        await act(async () => {
            copiedWorkflowUuid = await result.current.mutateAsync('template-uuid');
        });

        expect(copiedWorkflowUuid).toBe('copied-uuid');
        expect(connectedUserProjectWorkflowApiMock.copyFrontendWorkflowTemplate).toHaveBeenCalledWith({
            workflowUuid: 'template-uuid',
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('deprovisions a reference and refreshes the automations', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(useDeprovisionReferenceMutation);

        await act(async () => {
            await result.current.mutateAsync('reference-uuid');
        });

        expect(connectedUserProjectWorkflowApiMock.deprovisionFrontendWorkflowReference).toHaveBeenCalledWith({
            workflowUuid: 'reference-uuid',
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('enables an automation', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(useSetAutomationEnabledMutation);

        await act(async () => {
            await result.current.mutateAsync({enabled: true, workflowUuid: 'workflow-uuid'});
        });

        expect(connectedUserProjectWorkflowApiMock.enableFrontendProjectWorkflow).toHaveBeenCalledWith({
            workflowUuid: 'workflow-uuid',
        });
        expect(connectedUserProjectWorkflowApiMock.disableFrontendProjectWorkflow).not.toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('disables an automation', async () => {
        const {result} = renderMutationHook(useSetAutomationEnabledMutation);

        await act(async () => {
            await result.current.mutateAsync({enabled: false, workflowUuid: 'workflow-uuid'});
        });

        expect(connectedUserProjectWorkflowApiMock.disableFrontendProjectWorkflow).toHaveBeenCalledWith({
            workflowUuid: 'workflow-uuid',
        });
        expect(connectedUserProjectWorkflowApiMock.enableFrontendProjectWorkflow).not.toHaveBeenCalled();
    });

    it('deletes an automation and refreshes the automations', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(useDeleteAutomationMutation);

        await act(async () => {
            await result.current.mutateAsync('workflow-uuid');
        });

        expect(connectedUserProjectWorkflowApiMock.deleteFrontendProjectWorkflow).toHaveBeenCalledWith({
            workflowUuid: 'workflow-uuid',
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('publishes an automation with an empty description and refreshes the automations', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(usePublishAutomationMutation);

        await act(async () => {
            await result.current.mutateAsync('workflow-uuid');
        });

        expect(connectedUserProjectWorkflowApiMock.publishFrontendProjectWorkflow).toHaveBeenCalledWith({
            publishFrontendProjectWorkflowRequest: {description: ''},
            workflowUuid: 'workflow-uuid',
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('updates the inputs of an automation without refreshing the automations', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(useUpdateAutomationInputsMutation);

        await act(async () => {
            await result.current.mutateAsync({inputs: {channel: 'general'}, workflowUuid: 'workflow-uuid'});
        });

        expect(connectedUserProjectWorkflowApiMock.updateFrontendProjectWorkflowInputs).toHaveBeenCalledWith({
            updateWorkflowInputsRequest: {inputs: {channel: 'general'}},
            workflowUuid: 'workflow-uuid',
        });
        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
    });

    it('creates a blank automation from the blank definition and refreshes the automations', async () => {
        connectedUserProjectWorkflowApiMock.createFrontendProjectWorkflow.mockResolvedValue('new-uuid');

        const {invalidateQueriesSpy, result} = renderMutationHook(useCreateBlankAutomationMutation);

        let createdWorkflowUuid: string | undefined;

        await act(async () => {
            createdWorkflowUuid = await result.current.mutateAsync();
        });

        expect(createdWorkflowUuid).toBe('new-uuid');
        expect(connectedUserProjectWorkflowApiMock.createFrontendProjectWorkflow).toHaveBeenCalledWith({
            createFrontendProjectWorkflowRequest: {definition: BLANK_DEFINITION},
        });
        expect(JSON.parse(BLANK_DEFINITION)).toEqual({
            description: '',
            label: 'New automation',
            tasks: [],
            triggers: [],
        });
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.automations});
    });

    it('wires a connection into a workflow node', async () => {
        const {result} = renderMutationHook(useWireNodeConnectionMutation);

        await act(async () => {
            await result.current.mutateAsync({
                connectionId: 42,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'slack_1',
                workflowUuid: 'workflow-uuid',
            });
        });

        expect(connectedUserProjectWorkflowApiMock.updateFrontendWorkflowConfigurationConnection).toHaveBeenCalledWith({
            updateFrontendWorkflowConfigurationConnectionRequest: {connectionId: 42},
            workflowConnectionKey: 'slack',
            workflowNodeName: 'slack_1',
            workflowUuid: 'workflow-uuid',
        });
    });

    it('deletes a connection and refreshes the connections', async () => {
        const {invalidateQueriesSpy, result} = renderMutationHook(useDeleteHubConnectionMutation);

        await act(async () => {
            await result.current.mutateAsync(7);
        });

        expect(connectionApiMock.deleteFrontendConnection).toHaveBeenCalledWith({id: 7});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: AutomationHubKeys.connections});
    });
});
