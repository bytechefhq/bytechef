import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import usePropertyCodeEditorDialogRightPanelConnectionsSelect from '../hooks/usePropertyCodeEditorDialogRightPanelConnectionsSelect';

const hoisted = vi.hoisted(() => ({
    saveClusterElementConnectionMutate: vi.fn(),
    saveWorkflowNodeConnectionMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveClusterElementTestConfigurationConnectionMutation: () => ({
        mutate: hoisted.saveClusterElementConnectionMutate,
    }),
    useSaveWorkflowTestConfigurationConnectionMutation: () => ({mutate: hoisted.saveWorkflowNodeConnectionMutate}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {connectionTags: [], connections: []},
        useCreateConnectionMutation: vi.fn(),
        useGetComponentDefinitionsQuery: () => ({data: undefined}),
        useGetConnectionTagsQuery: vi.fn(),
        useGetConnectionsQuery: () => ({data: undefined}),
    }),
}));

const createWrapper =
    (readOnly: boolean) =>
    ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={new QueryClient()}>
            <WorkflowEditorReadOnlyContext.Provider value={readOnly}>{children}</WorkflowEditorReadOnlyContext.Provider>
        </QueryClientProvider>
    );

const renderSelectHook = (readOnly: boolean) =>
    renderHook(
        () =>
            usePropertyCodeEditorDialogRightPanelConnectionsSelect({
                componentConnection: {
                    componentName: 'slack',
                    componentVersion: 1,
                    key: 'slack',
                    required: true,
                    workflowNodeName: 'script_1',
                },
                workflowId: 'workflow-1',
                workflowNodeName: 'script_1',
            }),
        {wrapper: createWrapper(readOnly)}
    );

describe('usePropertyCodeEditorDialogRightPanelConnectionsSelect in read-only mode', () => {
    beforeEach(() => {
        hoisted.saveClusterElementConnectionMutate.mockReset();
        hoisted.saveWorkflowNodeConnectionMutate.mockReset();

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'script_1', workflowNodeName: 'script_1'},
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('saves the selected test connection when the editor is editable', () => {
        const {result} = renderSelectHook(false);

        act(() => result.current.handleValueChange(7, 'slack'));

        expect(hoisted.saveWorkflowNodeConnectionMutate).toHaveBeenCalledTimes(1);
    });

    it('never saves a selected test connection in read-only mode', () => {
        const {result} = renderSelectHook(true);

        act(() => result.current.handleValueChange(7, 'slack'));

        expect(hoisted.saveWorkflowNodeConnectionMutate).not.toHaveBeenCalled();
        expect(hoisted.saveClusterElementConnectionMutate).not.toHaveBeenCalled();
    });
});
