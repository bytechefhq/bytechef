import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ROOT_CLUSTER_ELEMENT_NAMES} from '@/shared/constants';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

interface HandleComponentAddedSuccessProps {
    nodeData: NodeDataType;
    queryClient: QueryClient;
    workflow: Workflow;
}

export default function handleComponentAddedSuccess({
    nodeData,
    queryClient,
    workflow,
}: HandleComponentAddedSuccessProps) {
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore.getState();

    const isRootClusterElement = ROOT_CLUSTER_ELEMENT_NAMES.includes(nodeData.componentName as string);

    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        }),
    });

    if (useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen) {
        if (currentNode?.trigger && nodeData.trigger) {
            setCurrentNode({...currentNode, ...nodeData});
            setCurrentComponent({...currentComponent, ...nodeData});
        }
    } else if (isRootClusterElement) {
        return;
    } else {
        setCurrentNode({...nodeData, description: ''});
        setCurrentComponent({...nodeData, description: ''});
        setWorkflowNodeDetailsPanelOpen(true);
    }
}
