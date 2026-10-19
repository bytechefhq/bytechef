import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useClusterElementsViewModeStore from '@/pages/platform/workflow-editor/stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {NodeDataType} from '@/shared/types';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useClusterElementsCanvasDialog from './useClusterElementsCanvasDialog';

const AI_AGENT_ROOT_DATA = {
    componentName: 'aiAgent',
    name: 'aiAgent_1',
    type: 'aiAgent/v1/chat',
    workflowNodeName: 'aiAgent_1',
} as unknown as NodeDataType;

describe('useClusterElementsCanvasDialog - editor preference restoration', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({workflow: {definition: undefined, id: 'workflow-1', nodeNames: [], version: 1}});
        useWorkflowEditorStore.setState({rootClusterElementNodeData: AI_AGENT_ROOT_DATA});
        useClusterElementsCanvasDialogStore.setState({
            aiAgentSimpleEditorPreferred: false,
            showAiAgentEditor: true,
        });
    });

    it('does not revert an explicitly-set showAiAgentEditor back to the stored preference in box mode', () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});

        renderHook(() => useClusterElementsCanvasDialog({onOpenChange: () => {}}));

        expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(true);
    });

    it('does restore showAiAgentEditor from the stored preference in dialog mode', () => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'dialog'});

        renderHook(() => useClusterElementsCanvasDialog({onOpenChange: () => {}}));

        expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(false);
    });
});

describe('useClusterElementsCanvasDialog - leaving the advanced editor in box mode', () => {
    beforeEach(() => {
        useClusterElementsViewModeStore.setState({clusterElementsViewMode: 'box'});
        useWorkflowDataStore.setState({workflow: {definition: undefined, id: 'workflow-1', nodeNames: [], version: 1}});
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});
        useWorkflowNodeDetailsPanelStore.setState({aiAgentNodeDetailsPanelOpen: true});
        useClusterElementsCanvasDialogStore.setState({
            aiAgentSimpleEditorPreferred: true,
            dataStreamSimpleEditorPreferred: true,
            showAiAgentEditor: true,
            showDataStreamEditor: true,
        });
    });

    it('closes the canvas back to the inline frame and remembers the AI agent preference', () => {
        useWorkflowEditorStore.setState({rootClusterElementNodeData: AI_AGENT_ROOT_DATA});

        const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: () => {}}));

        act(() => result.current.handleToggleEditor(false));

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
        expect(useWorkflowNodeDetailsPanelStore.getState().aiAgentNodeDetailsPanelOpen).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().aiAgentSimpleEditorPreferred).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().showAiAgentEditor).toBe(true);
    });

    it('closes the canvas back to the inline frame and remembers the data stream preference', () => {
        useWorkflowEditorStore.setState({
            rootClusterElementNodeData: {
                ...AI_AGENT_ROOT_DATA,
                componentName: 'dataStream',
                name: 'dataStream_1',
                workflowNodeName: 'dataStream_1',
            } as NodeDataType,
        });

        const {result} = renderHook(() => useClusterElementsCanvasDialog({onOpenChange: () => {}}));

        act(() => result.current.handleToggleEditor(false));

        expect(useWorkflowEditorStore.getState().clusterElementsCanvasOpen).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().dataStreamSimpleEditorPreferred).toBe(false);
        expect(useClusterElementsCanvasDialogStore.getState().aiAgentSimpleEditorPreferred).toBe(true);
    });
});
