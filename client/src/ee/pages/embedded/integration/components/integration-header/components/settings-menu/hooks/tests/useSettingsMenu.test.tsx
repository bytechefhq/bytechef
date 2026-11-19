import {Integration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider, QueryObserver} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useSettingsMenu} from '../useSettingsMenu';

const hoisted = vi.hoisted(() => ({
    clearWorkflowHistory: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    deleteWorkflowOptions: undefined as any,
    navigate: vi.fn(),
    noopMutation: () => ({isPending: false, mutate: vi.fn()}),
    restoreIntegrationVersionMutate: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    restoreIntegrationVersionOptions: undefined as any,
    toast: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    clearWorkflowHistory: hoisted.clearWorkflowHistory,
}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    useDeleteIntegrationMutation: hoisted.noopMutation,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useRestoreIntegrationVersionMutation: (options: any) => {
        hoisted.restoreIntegrationVersionOptions = options;

        return {isPending: false, mutate: hoisted.restoreIntegrationVersionMutate};
    },
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: hoisted.noopMutation,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useDeleteWorkflowMutation: (options: any) => {
        hoisted.deleteWorkflowOptions = options;

        return {isPending: false, mutate: vi.fn()};
    },
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationWorkflowImported: vi.fn()}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams('environmentId=1')],
}));

vi.mock('sonner', () => ({toast: hoisted.toast}));

const integration: Integration = {
    componentName: 'component',
    id: 7,
    integrationWorkflowIds: [11, 12],
    multipleInstances: false,
};
const workflow: Workflow = {id: 'workflow-1', integrationWorkflowId: 11};

describe('useSettingsMenu', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();

        hoisted.clearWorkflowHistory.mockReset();
        hoisted.navigate.mockReset();
        hoisted.restoreIntegrationVersionMutate.mockReset();
        hoisted.toast.mockReset();
    });

    const renderSettingsMenu = () =>
        renderHook(() => useSettingsMenu({integration, workflow}), {
            wrapper: ({children}: {children: ReactNode}) => (
                <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
            ),
        });

    it('restores the given version of the integration', () => {
        const {result} = renderSettingsMenu();
        const onSuccess = vi.fn();

        result.current.handleRestoreIntegrationVersion(3, onSuccess);

        expect(hoisted.restoreIntegrationVersionMutate).toHaveBeenCalledWith(
            {id: 7, integrationVersion: 3},
            {onSuccess}
        );
        expect(result.current.restoreIntegrationVersionPending).toBe(false);
    });

    it('stays on the current workflow and refreshes the integration and the workflow when the restored version still contains it', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...integration, integrationWorkflowIds: [11, 13]});

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreIntegrationVersionOptions.onSuccess(undefined, {id: 7, integrationVersion: 1});
        });

        expect(hoisted.navigate).not.toHaveBeenCalled();
        expect(hoisted.clearWorkflowHistory).toHaveBeenCalled();
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrations', 7]});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrations', 7, 'integrationWorkflows', 11]});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ['integrationVersions', 'integrations', 7],
        });
        expect(hoisted.toast).toHaveBeenCalledWith('Version V1 applied to the draft.');
    });

    it('closes the node details panel after a version is restored', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...integration, integrationWorkflowIds: [11, 13]});

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'dataStorage_1'} as NodeDataType,
            workflowNodeDetailsPanelOpen: true,
        });

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreIntegrationVersionOptions.onSuccess(undefined, {id: 7, integrationVersion: 1});
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
    });

    it('opens the first restored workflow when the restored version no longer contains the current one', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...integration, integrationWorkflowIds: [13, 14]});

        const removeQueriesSpy = vi.spyOn(queryClient, 'removeQueries');

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreIntegrationVersionOptions.onSuccess(undefined, {id: 7, integrationVersion: 1});
        });

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/embedded/integrations/7/integration-workflows/13?environmentId=1'
        );
        expect(removeQueriesSpy).toHaveBeenCalledWith({queryKey: ['integrations', 7, 'integrationWorkflows', 11]});
    });

    it('returns to the integration list when the restored version has no workflows', async () => {
        vi.spyOn(queryClient, 'query').mockResolvedValue({...integration, integrationWorkflowIds: []});

        renderSettingsMenu();

        await act(async () => {
            await hoisted.restoreIntegrationVersionOptions.onSuccess(undefined, {id: 7, integrationVersion: 1});
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations');
    });

    it('does not fetch the deleted workflow again while the editor still shows it', async () => {
        const deletedWorkflowQueryFn = vi.fn().mockResolvedValue(workflow);
        const deletedWorkflowObserverOptions = {
            queryFn: deletedWorkflowQueryFn,
            queryKey: ['integrations', 7, 'integrationWorkflows', 11],
            staleTime: Infinity,
        };

        queryClient.setQueryData(['integrations', 7, 'integrationWorkflows', 11], workflow);

        const deletedWorkflowObserver = new QueryObserver(queryClient, deletedWorkflowObserverOptions);

        const unsubscribe = deletedWorkflowObserver.subscribe(() => {});

        queryClient.setQueryData(['integrations', 7], integration);

        renderSettingsMenu();

        await act(async () => {
            hoisted.deleteWorkflowOptions.onSuccess();

            deletedWorkflowObserver.setOptions(deletedWorkflowObserverOptions);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/embedded/integrations/7/integration-workflows/12?environmentId=1'
        );
        expect(deletedWorkflowQueryFn).not.toHaveBeenCalled();
        expect(queryClient.getQueryState(['integrations', 7])?.isInvalidated).toBe(true);

        unsubscribe();
    });
});
