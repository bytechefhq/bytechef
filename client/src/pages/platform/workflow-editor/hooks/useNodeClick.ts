import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {NodeDataType, TabNameType} from '@/shared/types';
import {NodeProps, useReactFlow} from '@xyflow/react';
import {useCallback} from 'react';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeDataType, id: NodeProps['id'], activeTab?: TabNameType) {
    const {setActiveTab, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const clickedNode = getNode(id);

        if (!clickedNode) {
            return;
        }

        setRightSidebarOpen(false);

        setActiveTab(activeTab ?? 'description');
        setCurrentNode(data);
        setWorkflowNodeDetailsPanelOpen(true);

        if (data.type) {
            setCurrentComponent({
                ...data,
                workflowNodeName: data.name,
            });
        }
    }, [
        getNode,
        id,
        setRightSidebarOpen,
        setWorkflowNodeDetailsPanelOpen,
        setCurrentNode,
        data,
        activeTab,
        setActiveTab,
        setCurrentComponent,
    ]);
}
