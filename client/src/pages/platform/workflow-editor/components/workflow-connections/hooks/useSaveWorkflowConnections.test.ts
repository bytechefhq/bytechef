import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {WorkflowConnectionChangeI} from '../utils/workflowConnectionGroups';
import useSaveWorkflowConnections, {
    SaveWorkflowConnectionsResultI,
    useSaveWorkflowConnectionsStore,
} from './useSaveWorkflowConnections';

const hoisted = vi.hoisted(() => ({
    deleteMutateAsyncMock: vi.fn(),
    saveMutateAsyncMock: vi.fn(),
    toastErrorMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveWorkflowTestConfigurationConnectionMutation: () => ({mutateAsync: hoisted.saveMutateAsyncMock}),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useDeleteWorkflowTestConfigurationConnectionMutation: () => ({mutateAsync: hoisted.deleteMutateAsyncMock}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({ConnectionKeys: {connections: ['connections']}}),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastErrorMock},
}));

interface DeferredI<T> {
    promise: Promise<T>;
    reject: (reason?: unknown) => void;
    resolve: (value: T) => void;
}

function createDeferred<T = void>(): DeferredI<T> {
    let resolve!: (value: T) => void;
    let reject!: (reason?: unknown) => void;

    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });

    return {promise, reject, resolve};
}

function createChange(
    workflowNodeName: string,
    connectionId: number | undefined,
    previousConnectionId?: number,
    workflowConnectionKey = 'googleSheets'
): WorkflowConnectionChangeI {
    return {connectionId, previousConnectionId, workflowConnectionKey, workflowNodeName};
}

function createNode(workflowNodeName: string, overrides: Partial<NodeDataType> = {}): NodeDataType {
    return {componentName: 'googleSheets', name: workflowNodeName, workflowNodeName, ...overrides};
}

let queryClient: QueryClient;

function renderSaveHook() {
    const wrapper = ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);

    return renderHook(() => useSaveWorkflowConnections({environmentId: 1, workflowId: 'workflow-1'}), {wrapper});
}

function countInvalidations(queryKey: readonly string[]) {
    const invalidateQueriesMock = vi.mocked(queryClient.invalidateQueries);

    return invalidateQueriesMock.mock.calls.filter(([filters]) => {
        const invalidatedKey = (filters as {queryKey?: readonly unknown[]} | undefined)?.queryKey;

        return JSON.stringify(invalidatedKey) === JSON.stringify(queryKey);
    }).length;
}

beforeEach(() => {
    queryClient = new QueryClient();

    vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);
    vi.spyOn(queryClient, 'removeQueries');

    hoisted.deleteMutateAsyncMock.mockResolvedValue(undefined);
    hoisted.saveMutateAsyncMock.mockResolvedValue({saveWorkflowTestConfigurationConnection: true});

    useSaveWorkflowConnectionsStore.setState({pendingBatchCount: 0});
    useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('useSaveWorkflowConnections', () => {
    it('saves changes one at a time', async () => {
        const firstSave = createDeferred();
        const secondSave = createDeferred();

        hoisted.saveMutateAsyncMock.mockReturnValueOnce(firstSave.promise).mockReturnValueOnce(secondSave.promise);

        const {result} = renderSaveHook();

        let batchPromise: Promise<SaveWorkflowConnectionsResultI> | undefined;

        act(() => {
            batchPromise = result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12),
            ]);
        });

        await waitFor(() => expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(1));

        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledWith({
            connectionId: 12,
            environmentId: 1,
            workflowConnectionKey: 'googleSheets',
            workflowId: 'workflow-1',
            workflowNodeName: 'googleSheets_1',
        });

        await act(async () => {});

        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(1);

        await act(async () => firstSave.resolve());

        await waitFor(() => expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(2));

        expect(hoisted.saveMutateAsyncMock).toHaveBeenLastCalledWith({
            connectionId: 12,
            environmentId: 1,
            workflowConnectionKey: 'googleSheets',
            workflowId: 'workflow-1',
            workflowNodeName: 'googleSheets_2',
        });

        await act(async () => secondSave.resolve());

        await expect(batchPromise).resolves.toEqual({failedChange: undefined, savedCount: 2});
    });

    it('queues a second batch behind the first', async () => {
        const firstBatchRefresh = createDeferred();

        vi.mocked(queryClient.invalidateQueries).mockImplementation((filters) =>
            filters?.queryKey?.[0] === 'workflowTestConfigurations' ? firstBatchRefresh.promise : Promise.resolve()
        );

        const {result} = renderSaveHook();

        let firstBatchPromise: Promise<SaveWorkflowConnectionsResultI> | undefined;
        let secondBatchPromise: Promise<SaveWorkflowConnectionsResultI> | undefined;

        act(() => {
            firstBatchPromise = result.current.saveWorkflowConnections([createChange('googleSheets_1', 12)]);
            secondBatchPromise = result.current.saveWorkflowConnections([
                createChange('slack_1', 4, undefined, 'slack'),
            ]);
        });

        await waitFor(() => expect(countInvalidations(['workflowTestConfigurations'])).toBe(1));

        await act(async () => {});

        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(1);
        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledWith(
            expect.objectContaining({workflowNodeName: 'googleSheets_1'})
        );

        vi.mocked(queryClient.invalidateQueries).mockResolvedValue(undefined);

        await act(async () => firstBatchRefresh.resolve());

        await waitFor(() => expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(2));

        expect(hoisted.saveMutateAsyncMock).toHaveBeenLastCalledWith(
            expect.objectContaining({workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'})
        );

        await expect(firstBatchPromise).resolves.toEqual({failedChange: undefined, savedCount: 1});
        await expect(secondBatchPromise).resolves.toEqual({failedChange: undefined, savedCount: 1});
    });

    it('queues a batch from a remounted sheet behind one still running', async () => {
        const firstSave = createDeferred();

        hoisted.saveMutateAsyncMock.mockReturnValueOnce(firstSave.promise);

        const closedSheet = renderSaveHook();

        let firstBatchPromise: Promise<SaveWorkflowConnectionsResultI> | undefined;

        act(() => {
            firstBatchPromise = closedSheet.result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12),
            ]);
        });

        await waitFor(() => expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(1));

        closedSheet.unmount();

        const reopenedSheet = renderSaveHook();

        let secondBatchPromise: Promise<SaveWorkflowConnectionsResultI> | undefined;

        act(() => {
            secondBatchPromise = reopenedSheet.result.current.saveWorkflowConnections([
                createChange('googleSheets_2', 12),
            ]);
        });

        await act(async () => {});

        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(1);

        await act(async () => firstSave.resolve());

        await waitFor(() => expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(2));

        expect(hoisted.saveMutateAsyncMock).toHaveBeenLastCalledWith(
            expect.objectContaining({workflowNodeName: 'googleSheets_2'})
        );

        await expect(firstBatchPromise).resolves.toEqual({failedChange: undefined, savedCount: 1});
        await expect(secondBatchPromise).resolves.toEqual({failedChange: undefined, savedCount: 1});
    });

    it('shares the pending state with every mounted hook', async () => {
        const save = createDeferred();

        hoisted.saveMutateAsyncMock.mockReturnValueOnce(save.promise);

        const savingSheet = renderSaveHook();
        const sheetShell = renderSaveHook();

        act(() => {
            savingSheet.result.current.saveWorkflowConnections([createChange('googleSheets_1', 12)]);
        });

        try {
            expect(sheetShell.result.current.pending).toBe(true);
        } finally {
            // Release the module-level queue even when the assertion fails, so later tests can save.
            await act(async () => save.resolve());
        }

        await waitFor(() => expect(sheetShell.result.current.pending).toBe(false));
    });

    it('clears a connection with the delete mutation when connectionId is undefined', async () => {
        const {result} = renderSaveHook();

        await act(async () => {
            await result.current.saveWorkflowConnections([createChange('googleSheets_1', undefined, 7)]);
        });

        expect(hoisted.deleteMutateAsyncMock).toHaveBeenCalledWith({
            deleteWorkflowTestConfigurationConnectionRequest: {connectionId: 7},
            environmentId: 1,
            workflowConnectionKey: 'googleSheets',
            workflowId: 'workflow-1',
            workflowNodeName: 'googleSheets_1',
        });
        expect(hoisted.saveMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('stops at the first failure and reports how many changes were saved', async () => {
        const changes = [
            createChange('googleSheets_1', 12, 7),
            createChange('googleSheets_2', 12, 7),
            createChange('googleSheets_3', 12),
        ];

        hoisted.saveMutateAsyncMock
            .mockResolvedValueOnce({saveWorkflowTestConfigurationConnection: true})
            .mockRejectedValueOnce(new Error('Optimistic lock failure'));

        const {result} = renderSaveHook();

        let saveResult: SaveWorkflowConnectionsResultI | undefined;

        await act(async () => {
            saveResult = await result.current.saveWorkflowConnections(changes);
        });

        expect(hoisted.saveMutateAsyncMock).toHaveBeenCalledTimes(2);
        expect(hoisted.saveMutateAsyncMock).not.toHaveBeenCalledWith(
            expect.objectContaining({workflowNodeName: 'googleSheets_3'})
        );
        expect(saveResult).toEqual({failedChange: changes[1], savedCount: 1});
        expect(result.current.pending).toBe(false);
    });

    it('toasts partial progress only when something was saved before the failure', async () => {
        hoisted.saveMutateAsyncMock
            .mockResolvedValueOnce({saveWorkflowTestConfigurationConnection: true})
            .mockRejectedValueOnce(new Error('Optimistic lock failure'));

        const {result} = renderSaveHook();

        await act(async () => {
            await result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12, 7),
                createChange('googleSheets_3', 12),
            ]);
        });

        expect(hoisted.toastErrorMock).toHaveBeenCalledTimes(1);
        expect(hoisted.toastErrorMock).toHaveBeenCalledWith("Updated 1 of 3 tasks. 2 tasks weren't changed.");

        hoisted.toastErrorMock.mockClear();
        hoisted.saveMutateAsyncMock
            .mockResolvedValueOnce({saveWorkflowTestConfigurationConnection: true})
            .mockResolvedValueOnce({saveWorkflowTestConfigurationConnection: true})
            .mockRejectedValueOnce(new Error('Optimistic lock failure'));

        await act(async () => {
            await result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12, 7),
                createChange('googleSheets_3', 12),
            ]);
        });

        expect(hoisted.toastErrorMock).toHaveBeenCalledWith("Updated 2 of 3 tasks. 1 task wasn't changed.");

        hoisted.toastErrorMock.mockClear();
        hoisted.saveMutateAsyncMock.mockRejectedValueOnce(new Error('Invalid connection'));

        let saveResult: SaveWorkflowConnectionsResultI | undefined;

        await act(async () => {
            saveResult = await result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12, 7),
            ]);
        });

        expect(saveResult).toEqual({failedChange: createChange('googleSheets_1', 12, 7), savedCount: 0});
        expect(hoisted.toastErrorMock).not.toHaveBeenCalled();
    });

    it('invalidates the caches once per batch', async () => {
        const {result} = renderSaveHook();

        await act(async () => {
            await result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12, 7),
                createChange('googleSheets_3', undefined, 9),
            ]);
        });

        expect(countInvalidations(['workflowTestConfigurations'])).toBe(1);
        expect(countInvalidations(['ValidateWorkflow'])).toBe(1);
        expect(countInvalidations(['connections'])).toBe(1);

        expect(queryClient.removeQueries).toHaveBeenCalledTimes(3);
        expect(queryClient.removeQueries).toHaveBeenCalledWith({
            queryKey: ['workflowNodeDynamicProperties', 'workflow-1'],
        });
        expect(queryClient.removeQueries).toHaveBeenCalledWith({queryKey: ['workflowNodeOptions', 'workflow-1']});
        expect(queryClient.removeQueries).toHaveBeenCalledWith({
            queryKey: ['clusterElementNodeOptions', 'workflow-1'],
        });
    });

    it('sends no request for an empty batch', async () => {
        const {result} = renderSaveHook();

        let saveResult: SaveWorkflowConnectionsResultI | undefined;

        await act(async () => {
            saveResult = await result.current.saveWorkflowConnections([]);
        });

        expect(saveResult).toEqual({savedCount: 0});
        expect(hoisted.saveMutateAsyncMock).not.toHaveBeenCalled();
        expect(queryClient.invalidateQueries).not.toHaveBeenCalled();
    });

    it("updates the open task panel's connectionId when a saved change targets it", async () => {
        useWorkflowNodeDetailsPanelStore.setState({currentNode: createNode('googleSheets_2', {connectionId: 7})});

        const {result} = renderSaveHook();

        await act(async () => {
            await result.current.saveWorkflowConnections([
                createChange('googleSheets_1', 12, 7),
                createChange('googleSheets_2', 12, 7),
            ]);
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toEqual(
            createNode('googleSheets_2', {connectionId: 12})
        );

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: createNode('openAi_1', {clusterElementType: 'model', componentName: 'openAi'}),
        });

        await act(async () => {
            await result.current.saveWorkflowConnections([createChange('aiAgent_1', 5, undefined, 'openAi_1')]);
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.connectionId).toBe(5);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: createNode('googleSheets_3', {connectionId: 9})});

        hoisted.saveMutateAsyncMock.mockRejectedValueOnce(new Error('Invalid connection'));

        await act(async () => {
            await result.current.saveWorkflowConnections([
                createChange('googleSheets_3', 12, 9),
                createChange('googleSheets_4', 12),
            ]);
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.connectionId).toBe(9);
    });

    it('clears the open task panel connectionId when its connection is cleared', async () => {
        useWorkflowNodeDetailsPanelStore.setState({currentNode: createNode('googleSheets_1', {connectionId: 7})});

        const {result} = renderSaveHook();

        await act(async () => {
            await result.current.saveWorkflowConnections([createChange('googleSheets_1', undefined, 7)]);
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.connectionId).toBeUndefined();
    });

    it('reports pending while a batch runs', async () => {
        const save = createDeferred();
        const testConfigurationRefresh = createDeferred();

        hoisted.saveMutateAsyncMock.mockReturnValueOnce(save.promise);

        vi.mocked(queryClient.invalidateQueries).mockImplementation((filters) =>
            filters?.queryKey?.[0] === 'workflowTestConfigurations'
                ? testConfigurationRefresh.promise
                : Promise.resolve()
        );

        const {result} = renderSaveHook();

        expect(result.current.pending).toBe(false);

        act(() => {
            result.current.saveWorkflowConnections([createChange('googleSheets_1', 12)]);
        });

        expect(result.current.pending).toBe(true);

        await act(async () => save.resolve());

        await waitFor(() => expect(countInvalidations(['workflowTestConfigurations'])).toBe(1));

        expect(result.current.pending).toBe(true);

        await act(async () => testConfigurationRefresh.resolve());

        await waitFor(() => expect(result.current.pending).toBe(false));
    });
});
