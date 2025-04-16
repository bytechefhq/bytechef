import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {NodeDataType, TabNameType} from '@/shared/types';
import {NodeProps} from '@xyflow/react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeDataType, id: NodeProps['id'], activeTab?: TabNameType) {
    const {setActiveTab, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const {setAiAgentOpen} = useWorkflowEditorStore();

    return useCallback(() => {
        const clickedNode = nodes.find((node) => node.id === id);

        if (!clickedNode) {
            return;
        }

        setRightSidebarOpen(false);
        setActiveTab(activeTab ?? 'description');
        setCurrentNode({...data, description: ''});

        if (data.componentName === 'aiAgent') {
            setAiAgentOpen(true);
            setWorkflowNodeDetailsPanelOpen(true);
        } else {
            setWorkflowNodeDetailsPanelOpen(true);
        }

        if (data.type) {
            setCurrentComponent({
                ...data,
                workflowNodeName: data.name,
            });
        }
    }, [
        nodes,
        setRightSidebarOpen,
        setActiveTab,
        activeTab,
        setCurrentNode,
        data,
        id,
        setAiAgentOpen,
        setWorkflowNodeDetailsPanelOpen,
        setCurrentComponent,
    ]);
}
