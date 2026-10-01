import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useClusterElementsViewModeStore from '@/pages/platform/workflow-editor/stores/useClusterElementsViewModeStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {NodeDataType} from '@/shared/types';
import {renderHook} from '@testing-library/react';
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
            editorPreferences: {'workflow-1:aiAgent_1': false},
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
