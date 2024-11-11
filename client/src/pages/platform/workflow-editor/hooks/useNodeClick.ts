import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import {useCallback} from 'react';
import {NodeProps, useReactFlow} from 'reactflow';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function useNodeClick(data: NodeProps['data'], id: NodeProps['id']) {
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

        setCurrentNode(data);

        if (data.type) {
            setCurrentComponent({
                componentName: data.componentName,
                description: data.description,
                displayConditions: data.displayConditions,
                label: data.label,
                metadata: data.metadata,
                operationName: data.operationName,
                parameters: data.parameters,
                type: data.type,
                workflowNodeName: data.name,
            });
        }
    }, [getNode, id, data, setRightSidebarOpen, setWorkflowNodeDetailsPanelOpen, setCurrentNode, setCurrentComponent]);
}
