import {useConnectionNoteStore} from '@/pages/platform/workflow-editor/stores/useConnectionNoteStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {useWorkflowBuilder} from '../useWorkflowBuilder';

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useParams: () => ({workflowUuid: 'workflow-uuid'}),
}));

vi.mock('@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries', () => ({
    ConnectedUserProjectWorkflowKeys: {connectedUserProjectWorkflow: (uuid: string) => ['workflow', uuid]},
    useGetConnectedUserProjectWorkflowQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    useUpdateWorkflowMutation: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/workflowNodeParameters.mutations', () => ({
    useDeleteClusterElementParameterMutation: () => ({mutate: vi.fn()}),
    useDeleteWorkflowNodeParameterMutation: () => ({mutate: vi.fn()}),
    useUpdateClusterElementParameterMutation: () => ({mutate: vi.fn()}),
    useUpdateWorkflowNodeParameterMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/platform/workflows.mutations', () => ({
    default: () => ({mutate: vi.fn()}),
}));

function renderCountedHook() {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const counter = {renders: 0};

    const wrapper = ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);

    renderHook(
        () => {
            counter.renders++;

            return useWorkflowBuilder();
        },
        {wrapper}
    );

    return counter;
}

describe('useWorkflowBuilder', () => {
    it.each([
        ['workflow data sampleOutputs', () => useWorkflowDataStore.getState().setSampleOutputs({trigger_1: {id: 1}})],
        [
            'node details panel currentNode',
            () =>
                useWorkflowNodeDetailsPanelStore
                    .getState()
                    .setCurrentNode({name: 'trigger_1', workflowNodeName: 'trigger_1'} as NodeDataType),
        ],
        ['data pill panel open state', () => useDataPillPanelStore.getState().setDataPillPanelOpen(true)],
        ['right sidebar open state', () => useRightSidebarStore.getState().setRightSidebarOpen(true)],
        ['connection note visibility', () => useConnectionNoteStore.getState().setShowConnectionNote(false)],
        ['workflow editor bottom panel', () => useWorkflowEditorStore.getState().setShowBottomPanelOpen(true)],
    ])('does not re-render when the %s changes', (_description, writeToStore) => {
        const counter = renderCountedHook();

        const rendersBeforeWrite = counter.renders;

        act(() => {
            writeToStore();
        });

        expect(counter.renders).toBe(rendersBeforeWrite);
    });

    it('re-renders when the workflow it reads changes', () => {
        const counter = renderCountedHook();

        const rendersBeforeWrite = counter.renders;

        act(() => {
            useWorkflowDataStore.getState().setWorkflow({id: 'workflow-2'});
        });

        expect(counter.renders).toBeGreaterThan(rendersBeforeWrite);
    });
});
