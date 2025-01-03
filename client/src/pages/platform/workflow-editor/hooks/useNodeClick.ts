import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {TabNameType} from '@/shared/types';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeProps['data'], id: NodeProps['id'], activeTab?: TabNameType) {
    const {setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setRightSidebarOpen} = useRightSidebarStore();

    const {getNode} = useReactFlow();

    return useCallback(() => {
        const clickedNode = getNode(id);

        if (!clickedNode) {
            return;
        }

        setRightSidebarOpen(false);

        setWorkflowNodeDetailsPanelOpen(true);

        setCurrentNode({...data, activeTab});

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
        setCurrentComponent,
    ]);
}
