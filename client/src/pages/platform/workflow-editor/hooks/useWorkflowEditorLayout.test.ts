import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowEditorLayout from './useWorkflowEditorLayout';

const aiAgentRootNode = {
    clusterElements: {tools: [{name: 'stripe_1', type: 'stripe/v1/createCustomer'}]},
    clusterRoot: true,
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
} satisfies NodeDataType;

beforeEach(() => {
    useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false, rootClusterElementNodeData: undefined});
    useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
});

describe('useWorkflowEditorLayout', () => {
    it('seeds the root cluster element node data when the canvas opens on a main cluster root', () => {
        renderHook(() => useWorkflowEditorLayout());

        act(() => {
            useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});
            useWorkflowNodeDetailsPanelStore.setState({currentNode: aiAgentRootNode});
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toEqual(aiAgentRootNode);
    });

    it('ignores a node that is not a main cluster root', () => {
        renderHook(() => useWorkflowEditorLayout());

        act(() => {
            useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {...aiAgentRootNode, clusterRoot: false} as NodeDataType,
            });
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();
    });

    it('ignores a nested cluster root', () => {
        renderHook(() => useWorkflowEditorLayout());

        act(() => {
            useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});
            useWorkflowNodeDetailsPanelStore.setState({
                currentNode: {...aiAgentRootNode, isNestedClusterRoot: true} as NodeDataType,
            });
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();
    });

    it('clears the cluster element state when the canvas is closed', () => {
        const {result} = renderHook(() => useWorkflowEditorLayout());

        act(() => {
            result.current.handleClusterElementsCanvasOpenChange(true);
            useWorkflowNodeDetailsPanelStore.setState({currentNode: aiAgentRootNode});
        });

        act(() => {
            result.current.handleClusterElementsCanvasOpenChange(false);
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();
        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
    });

    it('re-seeds when the canvas is reopened on the same cluster root', () => {
        const {result} = renderHook(() => useWorkflowEditorLayout());

        act(() => {
            result.current.handleClusterElementsCanvasOpenChange(true);
            useWorkflowNodeDetailsPanelStore.setState({currentNode: aiAgentRootNode});
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toEqual(aiAgentRootNode);

        act(() => {
            result.current.handleClusterElementsCanvasOpenChange(false);
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();

        act(() => {
            result.current.handleClusterElementsCanvasOpenChange(true);
        });

        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toEqual(aiAgentRootNode);
    });
});
