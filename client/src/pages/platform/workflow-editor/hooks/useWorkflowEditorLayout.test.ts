import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
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
    useWorkflowDataStore.setState({workflow: {nodeNames: []}});
    useWorkflowEditorStore.setState({
        clusterElementsCanvasOpen: false,
        mainClusterRootComponentDefinition: undefined,
        rootClusterElementNodeData: undefined,
    });
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

    it('closes a cluster elements canvas left open by a previous workflow when the editor mounts', () => {
        useWorkflowEditorStore.setState({
            clusterElementsCanvasOpen: true,
            mainClusterRootComponentDefinition: {name: 'aiAgent'} as ComponentDefinition,
            rootClusterElementNodeData: aiAgentRootNode,
        });

        renderHook(() => useWorkflowEditorLayout());

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();
        expect(useWorkflowEditorStore.getState().mainClusterRootComponentDefinition).toBeUndefined();
    });

    it('closes the cluster elements canvas when the editor switches to another workflow', () => {
        useWorkflowDataStore.setState({workflow: {id: 'workflow-a', nodeNames: []}});

        renderHook(() => useWorkflowEditorLayout());

        act(() => {
            useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});
            useWorkflowNodeDetailsPanelStore.setState({currentNode: aiAgentRootNode});
        });

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(true);

        act(() => {
            useWorkflowDataStore.setState({workflow: {id: 'workflow-b', nodeNames: []}});
        });

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
        expect(useWorkflowEditorStore.getState().rootClusterElementNodeData).toBeUndefined();
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
