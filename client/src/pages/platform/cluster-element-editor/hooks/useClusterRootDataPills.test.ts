import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {renderHook} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useGetPreviousWorkflowNodeOutputsQueryMock} = vi.hoisted(() => ({
    useGetPreviousWorkflowNodeOutputsQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    useGetPreviousWorkflowNodeOutputsQuery: useGetPreviousWorkflowNodeOutputsQueryMock,
}));

import useClusterRootDataPills from './useClusterRootDataPills';

describe('useClusterRootDataPills', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowDataStore.setState({
            componentDefinitions: [{name: 'chat', title: 'Chat', version: 1}],
            dataPills: [],
            taskDispatcherDefinitions: [],
            workflow: {id: 'workflow-1', nodeNames: []},
        } as never);

        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {componentName: 'aiAgent', name: 'aiAgent_1', workflowNodeName: 'aiAgent_1'},
        } as never);
    });

    it('requests the outputs of the nodes before the agent', () => {
        useGetPreviousWorkflowNodeOutputsQueryMock.mockReturnValue({data: undefined});

        renderHook(() => useClusterRootDataPills());

        expect(useGetPreviousWorkflowNodeOutputsQueryMock).toHaveBeenCalledWith(
            expect.objectContaining({id: 'workflow-1', lastWorkflowNodeName: 'aiAgent_1'}),
            true
        );
        expect(useWorkflowDataStore.getState().dataPills).toEqual([]);
    });

    it('publishes the data pills of the nodes before the agent', () => {
        useGetPreviousWorkflowNodeOutputsQueryMock.mockReturnValue({
            data: [
                {
                    outputResponse: {
                        outputSchema: {properties: [{name: 'message', type: 'STRING'}], type: 'OBJECT'},
                    },
                    triggerDefinition: {componentName: 'chat', name: 'newChatRequest'},
                    workflowNodeName: 'trigger_1',
                },
            ],
        });

        renderHook(() => useClusterRootDataPills());

        const dataPillValues = useWorkflowDataStore.getState().dataPills.map((dataPill) => dataPill.value);

        expect(dataPillValues).toEqual(['trigger_1', 'trigger_1.message']);
    });
});
